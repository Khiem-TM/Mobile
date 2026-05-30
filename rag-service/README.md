# VitalAI — RAG Core Service (FastAPI)

AI RAG service tư vấn dinh dưỡng / tập luyện / body-metrics / mental-health cho app VitalAI.
NestJS = Gateway/Business/Source-of-truth; service này = "bộ não" RAG. Chi tiết thiết kế xem
`/Users/user/.claude/plans/humming-pondering-crayon.md`.

## Kiến trúc (P1 — MVP)
- **Transport**: HTTP (`POST /chat`) — khớp `ChatbotService` của NestJS (đọc trường `reply`).
- **LLM**: OpenAI (`gpt-4o-mini`), fallback Gemini; chưa có key → chạy **stub** (dev).
- **Embedding**: **local** `intfloat/multilingual-e5-base` (768-dim) qua sentence-transformers.
- **Vector DB**: pgvector trong DB `calories_tracker`, schema `rag`; đọc business data read-only (`rag_reader`).
- Pipeline: `safety(pre) → intent → retrieve → memory → prompt → LLM → safety(post) → log`.

## Chạy local (Docker Compose)
```bash
# từ thư mục gốc repo
export OPENAI_API_KEY=sk-...        # bỏ qua nếu muốn chạy stub
export RAG_INTERNAL_SECRET=dev-secret-change-in-prod
docker compose up -d postgres redis rag-service

# DB đã tồn tại từ trước (volume cũ) → chạy init thủ công 1 lần:
psql "postgresql://postgres:123456@localhost:5433/calories_tracker" -f rag-service/scripts/init_db.sql

# nạp tài liệu kiến thức khởi đầu
python rag-service/scripts/seed_documents.py

# kiểm tra
curl localhost:8001/health
curl -X POST localhost:8001/chat -H 'X-Internal-Secret: dev-secret-change-in-prod' \
  -H 'Content-Type: application/json' \
  -d '{"user_id":"demo","message":"Tôi nên ăn gì để giảm cân?"}'
```

## Chạy local (không Docker)
```bash
cd rag-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env            # điền OPENAI_API_KEY nếu có
psql "postgresql://postgres:123456@localhost:5433/calories_tracker" -f scripts/init_db.sql
uvicorn app.main:app --reload --port 8001
pytest -q                        # unit tests (intent/safety/chunking)
```

## Tích hợp NestJS
`ChatbotService.sendMessage` build **UserContext** (đã mask PII, qua `AiContextService`) rồi
gọi `POST /chat` kèm `user_context` + `request_id`, timeout 30s + retry. Không đổi DB/contract
cũ (vẫn đọc `reply`).

## Migrations
- Greenfield: dùng `scripts/init_db.sql` (tạo extension + schema `rag` + `rag_reader`).
- Thay đổi về sau: `alembic` (sau init, chạy `alembic stamp head` rồi `alembic revision --autogenerate`).

## Endpoints
| Method | Path | Mô tả |
|---|---|---|
| POST | `/chat` | RAG chat (NestJS gọi; đọc `reply`). |
| POST | `/chat/stream` | SSE: sự kiện `meta` → `delta*` → `done`. |
| POST | `/embed/document` | Nạp tài liệu (chunk→embed→index). |
| POST | `/embed/user/{id}` | Trigger từ NestJS (P3 sẽ dùng Kafka). |
| POST/GET | `/summary/daily` · `/summary/daily/{user_ref}?date=` | Sinh/đọc tóm tắt ngày. |
| POST/GET | `/report/weekly` · `/report/weekly/{user_ref}?week_start=` | Sinh/đọc báo cáo tuần. |
| GET | `/health` · `/ready` · `/metrics` | Health + Prometheus. |

Mọi endpoint (trừ health/metrics) yêu cầu header `X-Internal-Secret`.

## Roadmap
- **P1 MVP** ✅ — chat RAG + safety + pgvector + embed local.
- **P2** ✅ — rolling conversation memory + daily/weekly summary + SSE streaming.
- P3 — Kafka event-driven (khung `kafka/` chưa tạo).
- P4 — gRPC (proto đã có `proto/rag.proto`).
- P5 — eval/monitoring/rerank. P6 — scale vector (HNSW/Qdrant).

> Đã smoke-test end-to-end với pgvector thật (seed → retrieve → chat → safety → log).
> Generation thật cần `OPENAI_API_KEY`; chưa có key → tự chạy chế độ **stub**.
