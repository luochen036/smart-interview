import json
import os
import time

import pika
import requests

RABBITMQ_HOST = os.environ.get("RABBITMQ_HOST", "localhost")
USER_SERVICE = os.environ.get("USER_SERVICE", "http://localhost:8081")
EXCHANGE = "test.exchange"
QUEUE = "test.submitted"


def generate_report(test_id: int) -> None:
    """读取答卷、调用 AI 分析，并把报告可靠地回写到用户服务。"""
    record_response = requests.get(f"{USER_SERVICE}/tests/{test_id}", timeout=15)
    record_response.raise_for_status()
    record = record_response.json()
    questions = json.loads(record.get("questions") or "[]")
    answers = json.loads(record.get("answers") or "[]")
    pairs = [
        {"question": question, "answer": answers[index] if index < len(answers) else ""}
        for index, question in enumerate(questions)
    ]

    report_response = requests.post(
        "http://localhost:8000/ai/test-report",
        json={"pairs": pairs},
        timeout=90,
    )
    report_response.raise_for_status()
    result = report_response.json()

    save_response = requests.post(
        f"{USER_SERVICE}/tests/{test_id}/report",
        json={"score": result["score"], "report": result["report"]},
        timeout=30,
    )
    save_response.raise_for_status()


def on_message(channel, method, properties, body) -> None:
    try:
        test_id = int(body)
        print(f"[测评] 收到测试 {test_id}，开始生成报告", flush=True)
        generate_report(test_id)
        channel.basic_ack(delivery_tag=method.delivery_tag)
        print(f"[测评] 测试 {test_id} 报告已回写", flush=True)
    except ValueError:
        print(f"[测评] 丢弃无法解析的旧格式消息：{body[:40]}", flush=True)
        channel.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as exception:
        # 处理失败时重新入队；连接恢复或下游服务就绪后会再次消费。
        print(f"[测评] 处理失败，将消息重新入队：{exception}", flush=True)
        channel.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
        time.sleep(3)


def consume_forever() -> None:
    """连接断开后持续重试，避免后台 Web 进程正常但消费者已经退出。"""
    while True:
        try:
            connection = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=RABBITMQ_HOST,
                    heartbeat=60,
                    blocked_connection_timeout=30,
                    connection_attempts=5,
                    retry_delay=3,
                )
            )
            channel = connection.channel()
            channel.exchange_declare(exchange=EXCHANGE, exchange_type="direct", durable=True)
            channel.queue_declare(queue=QUEUE, durable=True)
            channel.queue_bind(queue=QUEUE, exchange=EXCHANGE, routing_key=QUEUE)
            channel.basic_qos(prefetch_count=1)
            channel.basic_consume(queue=QUEUE, on_message_callback=on_message)
            print("[测评] 消费者已就绪，等待测试消息", flush=True)
            channel.start_consuming()
        except Exception as exception:
            print(f"[测评] RabbitMQ 连接中断，3 秒后重试：{exception}", flush=True)
            time.sleep(3)


if __name__ == "__main__":
    consume_forever()
