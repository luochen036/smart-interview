import json
import os
import time
import requests
from rest_framework.decorators import api_view
from rest_framework.response import Response

# ---------- 兜底版"AI"：没配 Key 也能跑 ----------
TEMPLATES = [
    ("Spring Boot", "请谈谈 Spring Boot 的自动装配原理。"),
    ("微服务", "你们的服务是怎么拆分、怎么互相调用的？"),
    ("Docker", "Docker 和虚拟机有什么区别？"),
    ("MySQL", "如何优化一条慢查询 SQL？"),
    ("Python", "Python 的 GIL 是什么？"),
]

def local_questions(resume: str):
    qs = [q for key, q in TEMPLATES if key in resume]
    if not qs:
        qs = ["请简单介绍一下你的项目经历。",
              "你最近学的一个新技术是什么？",
              "说说你遇到过最大的挑战。"]
    return qs[:3]

def local_score(answer: str):
    score = min(95, 55 + len(answer) // 10)   # 按内容长度给个基础分
    comment = "回答有内容！建议用 STAR 法则（情境-任务-行动-结果）组织，更有说服力。"
    return score, comment

# ---------- 真·AI 版：调用通义千问（可选） ----------
API_KEY = os.environ.get("DASHSCOPE_API_KEY", "")   # 没配就是空

def qwen(messages: list):
    r = requests.post(
        "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        headers={"Authorization": "Bearer " + API_KEY},
        json={"model": "qwen-plus", "messages": messages},
        timeout=30,
    )
    return r.json()["choices"][0]["message"]["content"]

def ai_questions(resume: str):
    reply = qwen([
        {"role": "system", "content": "你是资深技术面试官，只返回 3 道面试题，每行一道。"},
        {"role": "user", "content": "根据这份简历出题：\n" + resume},
    ])
    return [line.strip("- ") for line in reply.splitlines() if line.strip()][:3]

def ai_score(question: str, answer: str):
    reply = qwen([
        {"role": "system", "content": "你是面试官，请严格返回 JSON："
                                       '{"score": 0到100整数, "comment": "30字内点评"}'},
        {"role": "user", "content": f"题目：{question}\n回答：{answer}"},
    ])
    data = json.loads(reply)
    return data["score"], data["comment"]

# ---------- 接口（Django 视图函数） ----------
@api_view(["POST"])
def questions(request):
    time.sleep(0.5)   # 模拟思考时间（顺便给熔断演示留空间）
    resume = request.data.get("resume", "")
    if API_KEY:
        qs = ai_questions(resume)
    else:
        qs = local_questions(resume)
    return Response({"questions": qs})

@api_view(["POST"])
def score(request):
    time.sleep(0.8)
    question = request.data.get("question", "")
    answer = request.data.get("answer", "")
    if API_KEY:
        s, c = ai_score(question, answer)
    else:
        s, c = local_score(answer)
    return Response({"score": s, "comment": c})

@api_view(["POST"])
def test_report(request):
    pairs = request.data.get("pairs", [])   # [{"question":..., "answer":...}, ...]
    if API_KEY:
        reply = qwen([
            {"role": "system", "content": "你是面试导师，请严格返回 JSON："
                                           '{"score": 0到100整数, "report": "100字内分析：掌握情况、薄弱点、改进建议"}'},
            {"role": "user", "content": "根据这份答题情况分析：\n" + json.dumps(pairs, ensure_ascii=False)},
        ])
        return Response(json.loads(reply))
    # 兜底：按作答认真程度打分 + 给建议
    score = min(95, 50 + sum(min(6, len(p["answer"]) // 20) for p in pairs))
    report = ("本次共作答 " + str(len(pairs)) + " 道题，整体有思路；"
              "建议薄弱知识点回归题库再学一遍，并用 STAR 法则完善表达。")
    return Response({"score": score, "report": report})