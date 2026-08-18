import logging
import time
from rest_framework.decorators import api_view
from rest_framework.response import Response
from interviewer.tools.ai import (
    ai_questions,
    ai_score,
    ai_test_report,
    get_api_key,
    local_questions,
    local_score,
)

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("ai-service")

NO_KEY_NOTE = "未配置 AI_API_KEY/DASHSCOPE_API_KEY，当前返回本地规则结果"


# ---------- 接口（Django 视图函数） ----------
@api_view(["POST"])
def questions(request):
    time.sleep(0.5)   # 模拟思考时间（顺便给熔断演示留空间）
    resume = request.data.get("resume", "")
    if get_api_key():
        try:
            return Response({"questions": ai_questions(resume)})
        except Exception as exception:
            logger.warning("AI 出题失败，改用本地规则：%s", exception)
            return Response({"questions": local_questions(resume), "aiNote": str(exception)})
    return Response({"questions": local_questions(resume), "aiNote": NO_KEY_NOTE})

@api_view(["POST"])
def score(request):
    time.sleep(0.8)
    question = request.data.get("question", "")
    answer = request.data.get("answer", "")
    if get_api_key():
        try:
            s, c = ai_score(question, answer)
            return Response({"score": s, "comment": c})
        except Exception as exception:
            logger.warning("AI 评分失败，改用本地规则：%s", exception)
            s, c = local_score(answer)
            return Response({"score": s, "comment": c, "aiNote": str(exception)})
    s, c = local_score(answer)
    return Response({"score": s, "comment": c, "aiNote": NO_KEY_NOTE})

@api_view(["POST"])
def test_report(request):
    pairs = request.data.get("pairs", [])   # [{"question":..., "answer":...}, ...]
    if get_api_key():
        try:
            score, report = ai_test_report(pairs)
            return Response({"score": score, "report": report})
        except Exception as exception:
            # AI 调用失败时走本地兜底，避免异步报告一直卡在 reporting
            logger.warning("AI 生成报告失败，改用本地规则：%s", exception)
            fallback_note = str(exception)
    else:
        fallback_note = NO_KEY_NOTE
    # 兜底：按作答认真程度打分 + 给建议
    score = min(95, 50 + sum(min(6, len(p["answer"]) // 20) for p in pairs))
    report = ("本次共作答 " + str(len(pairs)) + " 道题，整体有思路；"
              "建议薄弱知识点回归题库再学一遍，并用 STAR 法则完善表达。")
    return Response({"score": score, "report": report, "aiNote": fallback_note})
