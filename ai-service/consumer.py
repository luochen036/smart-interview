import os
import json
import time
import pika
import requests

RABBITMQ_HOST = os.environ.get("RABBITMQ_HOST", "localhost")
USER_SERVICE = os.environ.get("USER_SERVICE", "http://localhost:8081")

def on_message(ch, method, properties, body):
    test_id = int(body)
    print(f"[x] 收到测试 {test_id}，开始生成报告…")
    # ① 从 user-service 取答卷
    rec = requests.get(f"{USER_SERVICE}/tests/{test_id}", timeout=10).json()
    qs = json.loads(rec.get("questions") or "[]")
    ans = json.loads(rec.get("answers") or "[]")
    pairs = [
        {"question": q, "answer": ans[i] if i < len(ans) else ""}
        for i, q in enumerate(qs)
    ]
    # ② 调我们自己 Django 的 /ai/test-report 做 AI 分析
    result = requests.post("http://localhost:8000/ai/test-report",
                           json={"pairs": pairs}, timeout=60).json()
    # ③ 把报告回写 user-service（个人中心就能看到）
    requests.post(f"{USER_SERVICE}/tests/{test_id}/report",
                  json={"score": result["score"], "report": result["report"]},
                  timeout=30)
    print(f"[x] 测试 {test_id} 报告已回写 ✅")
    ch.basic_ack(delivery_tag=method.delivery_tag)   # 确认签收

connection = pika.BlockingConnection(
    pika.ConnectionParameters(host=RABBITMQ_HOST))
channel = connection.channel()
# 声明交换机 + 队列 + 绑定（与 Java 端保持一致）
channel.exchange_declare(exchange="test.exchange", exchange_type="direct")
channel.queue_declare(queue="test.submitted", durable=True)
channel.queue_bind(queue="test.submitted",
                   exchange="test.exchange", routing_key="test.submitted")
channel.basic_qos(prefetch_count=1)   # 一次只取一条，慢慢消化
channel.basic_consume(queue="test.submitted",
                      on_message_callback=on_message)
print("🐰 [*] 消费者已就绪，等待测试消息……")
channel.start_consuming()