from django.urls import path
from . import views

urlpatterns = [
    path('questions/', views.questions),   # POST /ai/questions
    path('score/', views.score),           # POST /ai/score
]