import json
import time
from rest_framework.decorators import api_view
from rest_framework.response import Response
from tools.ai import *


# ---------- 接口（Django 视图函数） ----------
@api_view(["POST"])
def questions(request):
    time.sleep(0.5)   # 模拟思考时间（顺便给熔断演示留空间）
    resume = request.data.get("resume", "")
    if API_KEY:
        try:
            qs = ai_questions(resume)
        except:
            qs = local_questions(resume)
    else:
        qs = local_questions(resume)
    return Response({"questions": qs})

@api_view(["POST"])
def score(request):
    time.sleep(0.8)
    question = request.data.get("question", "")
    answer = request.data.get("answer", "")
    if API_KEY:
        try:
            s, c = ai_score(question, answer)
        except:
            s, c = local_score(answer)
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