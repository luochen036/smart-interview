FROM python:3.12-slim
WORKDIR /app
# 换 pip 国内源（清华镜像），加速依赖下载
RUN pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["gunicorn", "ai_project.wsgi:application", "--bind", "0.0.0.0:8000"]