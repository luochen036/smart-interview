from django.urls import path
from . import views

urlpatterns = [
    path('questions', views.questions),        # POST /ai/questions（网关转发无尾斜杠，精确匹配）
    path('score', views.score),                # POST /ai/score
    path('test-report', views.test_report),    # POST /ai/test-report（MQ 消费者调用）
]