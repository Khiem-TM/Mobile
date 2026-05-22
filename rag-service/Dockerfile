FROM python:3.12-slim

WORKDIR /app

# Install uv for fast dependency installation
RUN pip install uv

# Copy dependency files first for layer caching
COPY pyproject.toml .
RUN uv pip install --system --no-cache-dir -e .

# Copy application code
COPY app/ ./app/
COPY knowledge/ ./knowledge/

# Create chroma_db directory
RUN mkdir -p /data/chroma

EXPOSE 8001

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
