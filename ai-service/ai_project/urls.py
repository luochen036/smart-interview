from django.urls import path, include

urlpatterns = [
    path('ai/', include('interviewer.urls')),   # 所有接口挂在 /ai/ 下
]