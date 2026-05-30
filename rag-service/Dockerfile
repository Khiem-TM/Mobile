FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    HF_HOME=/data/hf

WORKDIR /app

# build deps cho sentence-transformers/asyncpg
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.docker.txt .
RUN pip install --upgrade pip && pip install -r requirements.docker.txt

COPY . .

EXPOSE 8001

# 1 worker để model embedding chỉ load 1 lần (tăng workers cần thêm RAM/worker).
CMD ["gunicorn", "app.main:app", \
     "-k", "uvicorn.workers.UvicornWorker", \
     "-w", "1", "-b", "0.0.0.0:8001", \
     "--timeout", "120"]
