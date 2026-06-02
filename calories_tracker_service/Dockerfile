FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    HF_HOME=/data/hf \
    DEVICE=cpu

WORKDIR /app

# Runtime libs: libglib2.0-0 for opencv-headless, libgomp1 for torch;
# build-essential for any wheels that need compiling. curl for health probes.
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential curl libglib2.0-0 libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Install CPU-only torch/torchvision FIRST so ultralytics doesn't drag in CUDA.
RUN pip install --upgrade pip \
    && pip install --index-url https://download.pytorch.org/whl/cpu torch torchvision

COPY requirements.docker.txt .
RUN pip install -r requirements.docker.txt \
    # ultralytics drags in non-headless opencv-python (needs libGL/libxcb and
    # overwrites the headless cv2). Force a clean headless-only cv2 so the
    # slim image needs no X11/GL system libraries.
    && pip uninstall -y opencv-python opencv-python-headless \
    && pip install opencv-python-headless

# App code + committed YOLO weights (yolov8n-seg.pt). The two HF models download
# into HF_HOME (/data/hf) on first start; mount a volume there to cache them.
COPY . .

EXPOSE 8000

# 1 worker so the 3 models load exactly once; long timeout because CPU inference
# (segmentation + classification + depth) is slow.
CMD ["gunicorn", "app.main:app", \
     "-k", "uvicorn.workers.UvicornWorker", \
     "-w", "1", "-b", "0.0.0.0:8000", \
     "--timeout", "300"]
