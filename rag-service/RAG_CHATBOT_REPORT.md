# Báo cáo kỹ thuật — RAG Chatbot "Vital Coach" (VitalAI)

_Phiên bản 0.2 · cập nhật 2026-05-30 · phạm vi: FastAPI RAG Core Service + tích hợp NestJS + đánh giá & định hướng cho Mobile Production._

> Tài liệu thiết kế gốc (18 phần): `/Users/user/.claude/plans/humming-pondering-crayon.md`.
> Mã nguồn dịch vụ: `rag-service/`. Tích hợp backend: `backend/src/modules/chatbot/`.

---

## 1. Tổng quan & mục tiêu

VitalAI là app theo dõi calo / tập luyện (Kotlin Android ↔ NestJS + PostgreSQL). Mục tiêu là một **chatbot tư vấn sức khỏe an toàn** (dinh dưỡng, tập luyện, chỉ số cơ thể, thói quen, sức khỏe tinh thần ở mức hỗ trợ), tách thành **service AI độc lập bằng FastAPI** để:

- Không nhồi logic AI nặng vào NestJS (giữ NestJS = Gateway / Business / **Source of Truth**).
- Cô lập chi phí, bảo mật key, vòng đời mô hình, và khả năng scale riêng.
- Cá nhân hoá câu trả lời bằng **dữ liệu thật của người dùng** (đã mask PII) + **kiến thức được truy xuất** (RAG).

**Trạng thái hiện tại:** P1 (MVP RAG) + P2 (memory + summary + streaming) đã hoàn thành và **đã chạy thật end-to-end** với OpenAI key thật, embedding local, pgvector trên DB thật (xem §8).

---

## 2. Kiến trúc hệ thống

```
            ┌──────────────┐    HTTP POST /chat (+X-Internal-Secret, request_id)   ┌─────────────────────────────┐
 Mobile ──▶ │   NestJS     │ ──────────────────────────────────────────────────▶ │   FastAPI RAG Core (8001)   │
 (JWT)      │  Gateway     │     body: {user_id, message, history, user_context}    │                             │
            │  + Business  │ ◀────────────────── {reply, intent, sources, safety} ─ │  rag_orchestrator           │
            │  + SoT (PG)  │                                                        │   ├─ safety_guard (pre)     │
            └──────┬───────┘                                                        │   ├─ intent (rule)          │
                   │ AiContextService.buildUserContext()                            │   ├─ retriever → pgvector   │
                   │ (mask PII → UserContext v1)                                    │   ├─ prompt_builder (VI)    │
                   ▼                                                                │   ├─ llm_service (OpenAI)   │
            chat_sessions / chat_messages  ← SoT hội thoại                          │   ├─ safety_guard (post)    │
                                                                                    │   └─ user_memory/summary    │
   PostgreSQL `calories_tracker` (pgvector 0.8.2)                                   │  embedding_service (local)  │
   ├─ public: users, user_health_profile, meal_log*, training_session*, …          │  document_ingestion         │
   └─ schema `rag`: rag_documents, rag_chunks(vector 768), conversation_memories,   │  observability (logs+metrics)│
      user_ai_daily_summaries, user_ai_weekly_reports, retrieval_logs, prompt_logs  └──────────────┬──────────────┘
        ▲ ghi (rag_reader)                                                                          │ read-only (rag_reader)
        └──────────────────────────────────────────────────────────────────────────────────────────┘
   Redis (tùy chọn): cache embedding query · idempotency · rate-limit
```

**Nguyên tắc ranh giới dữ liệu:**
- Lịch sử chat = **NestJS sở hữu** (`chat_messages`). FastAPI **không** lưu nội dung thô; chỉ lưu *AI-derived data* (chunks, memory/summary, logs) trong schema `rag`.
- Business data: FastAPI **chỉ đọc** qua role `rag_reader` (không sửa).
- PII (email/phone/password/google id) **không bao giờ** rời NestJS.

---

## 3. Luồng xử lý (request flows)

### 3.1. Chat đồng bộ — `POST /chat`
1. **NestJS** (`ChatbotService.sendMessage`): nạp ≤20 lượt lịch sử → gọi `AiContextService.buildUserContext(userId)` (gom health-profile + dashboard hôm nay, **mask PII**, hash `user_id → user_ref`) → `callRag()` (timeout 30s, retry 1 lần cho lỗi mạng/5xx, không retry 4xx, fallback message thân thiện).
2. **FastAPI** (`rag_orchestrator.answer`):
   `safety pre-check` (khủng hoảng → chặn, trả safe-reply, **không gọi LLM**) → `intent` (rule) → `retriever` (embed query → vector search pgvector → filter theo `source` của intent) → `user_memory` (lấy rolling summary) → `prompt_builder` (ráp system VI + UserContext + summary + chunks + history) → `llm_service.generate` (OpenAI, fallback Gemini) → `safety post-check` (chèn disclaimer/cảnh báo) → **memory refresh** có điều kiện (mỗi 6 lượt) → ghi `retrieval_logs` + `prompt_logs` → trả `{reply, intent, sources, safety, usage}`.
3. **NestJS** lưu user message + assistant reply vào `chat_messages`, cập nhật tiêu đề/last_message.

### 3.2. Chat streaming — `POST /chat/stream` (SSE)
`orchestrator.prepare_stream` chạy các bước đồng bộ (safety/intent/retrieve/prompt + ghi `retrieval_logs`) **trong scope DB session**, rồi trả `StreamPlan`. Generator phát SSE: `event: meta` (intent + sources) → nhiều `event: delta` (token) → `event: done` (disclaimer). Câu khủng hoảng phát thẳng safe-reply, bỏ qua LLM.

### 3.3. Tóm tắt ngày / báo cáo tuần — `POST/GET /summary/daily`, `/report/weekly`
NestJS scheduler gửi `{user_ref, date, user_context}` → `summary_service` dùng LLM cô đọng dữ liệu thành văn bản VI ngắn gọn, lưu `user_ai_daily_summaries` / `user_ai_weekly_reports`. Mobile (qua NestJS) đọc bằng GET.

### 3.4. Nạp tài liệu — `POST /embed/document`
`document_ingestion`: clean → **chunk theo loại** (nutrition 500/overlap70, workout 400/40, mental 400/60, faq 1 chunk/Q&A) → embed (batch) → ghi `rag_documents` + `rag_chunks` (vector + metadata JSONB).

---

## 4. Tech stack

| Lớp | Công nghệ | Ghi chú |
|---|---|---|
| API service | **FastAPI** + Uvicorn/Gunicorn | 1 worker (model embed load 1 lần/worker) |
| LLM generation | **OpenAI `gpt-4o-mini`** (MVP) | fallback **Gemini 2.5 Flash**; key đã có sẵn; auto **stub** khi thiếu key |
| Embedding | **local `intfloat/multilingual-e5-base`** (768-dim) | sentence-transformers; tốt tiếng Việt; hash-fallback cho dev |
| Vector DB | **PostgreSQL 16 + pgvector 0.8.2** | schema `rag`, index IVFFlat (cosine) + GIN(metadata) |
| ORM / driver | SQLAlchemy 2 (async) + asyncpg | read-only business qua role `rag_reader` |
| Migration | Alembic (incremental) + `scripts/init_db.sql` (greenfield) | |
| Cache / idempotency | Redis (tùy chọn) | cache query-embedding, `processed_events` |
| Observability | structlog (JSON, mask PII) + prometheus-client | `/metrics`: latency, tokens, cost, retrieval hits, safety |
| Gateway/SoT | NestJS + TypeORM + PostgreSQL | giữ chat history, build UserContext, gọi RAG |
| Realtime (P4) | proto gRPC đã thiết kế (`proto/rag.proto`) | hiện dùng HTTP + SSE |
| Đóng gói | Docker / Docker Compose | postgres(pgvector) + redis + rag-service |

---

## 5. Cấu trúc mã & trách nhiệm module

```
rag-service/app/
├─ api/         chat.py (/chat, /chat/stream) · embed.py · reports.py · health.py
├─ core/        rag_orchestrator · retriever · prompt_builder · safety_guard
│               intent · embedding_service · llm_service · user_memory
│               summary_service · document_ingestion
├─ providers/   base (Protocol) · openai_llm · gemini_llm · stub_llm
│               local_embed · openai_embed · factory (degrade an toàn → stub)
├─ db/          session · models (schema rag) · rag_repo (vector search + logs)
│               business_repo (read-only public)
├─ schemas/     chat · reports (pydantic, UserContext-aware)
├─ observability/ logging · metrics
└─ config.py · deps.py · main.py
scripts/init_db.sql · scripts/seed_documents.py · proto/rag.proto · tests/
```

**Điểm thiết kế đáng chú ý:**
- **Provider abstraction**: đổi OpenAI↔Gemini↔local mà không sửa pipeline; `factory` bọc try/except để **không crash startup** khi thiếu dependency phụ.
- **Tách đồng bộ/stream**: `prepare_stream` giữ mọi thao tác DB trong scope session, tránh lỗi vòng đời khi SSE chạy sau khi handler trả về.
- **Heavy deps lazy import** (torch/openai/sentence-transformers nạp khi cần) → app khởi động nhẹ, test nhanh.

---

## 6. Mô hình dữ liệu

### 6.1. UserContext v1 (NestJS build, đã mask PII)
`{schema_version, user_ref(hash), profile{age,gender,height_cm,activity_level,diet_type, allergies, goal_type, target_weight_kg, daily_calories_goal, macro_goal_g, weekly_rate_kg}, today{date, calories_in, macros_g, water_ml, steps, sleep_h, mood}, latest_body{weight_kg,bmi}}`.
**Quan trọng:** `allergies` lấy từ `user_health_profile.food_allergies` → guardrail dùng để né món gây dị ứng.

### 6.2. Schema `rag` (pgvector)
`rag_documents`, `rag_chunks(embedding vector(768), metadata jsonb)`, `conversation_memories`, `user_ai_daily_summaries`, `user_ai_weekly_reports`, `retrieval_logs`, `prompt_logs`, `processed_events`.
Index: `ivfflat (embedding vector_cosine_ops) lists=100` + `gin(metadata)`.

---

## 7. An toàn (safety guardrails)

- **Pre-check input**: phát hiện tín hiệu khủng hoảng (tự hại, đau ngực, chấn thương nặng) → trả lời an toàn + khuyến nghị tìm hỗ trợ/115, **không gọi LLM**, không retrieval.
- **Post-check output**: bắt khuyến nghị nguy hiểm (nhịn ăn cực đoan, giảm >1kg/tuần, lạm dụng thuốc/supplement) → chèn cảnh báo; gắn **medical disclaimer** cho nutrition/workout/body; disclaimer riêng cho mental_health.
- **Prompt cứng**: "không phải bác sĩ", không chẩn đoán, tôn trọng dị ứng, chỉ dựa dữ liệu + chunks, "không biết thì nói".
- Mọi kết quả safety được ghi `prompt_logs.safety_result`.

---

## 8. Đã kiểm chứng (verified)

**Unit/import**: `compileall` sạch; `import app.main` OK; 5/5 unit test (intent, safety, chunking) pass; config đọc đúng `.env` thật (alias `OPENAI_MODEL`, strip comment inline).

**End-to-end THẬT** (pgvector 0.8.2 trên DB `calories_tracker`, embedding local e5, OpenAI key thật):

| Truy vấn | Intent | Nguồn truy xuất | Model | Tokens (in/out) | Cost |
|---|---|---|---|---|---|
| Bữa tối ~500 kcal giàu protein, dị ứng hải sản | nutrition | "Nguyên tắc giảm cân an toàn", "Phân bổ macro" | gpt-4o-mini | 621 / 149 | $0.000183 |
| Người mới tập gym an toàn | workout | "Khởi động và phòng chấn thương" | gpt-4o-mini | 545 / 185 | $0.000193 |
| "...muốn tự tử" | mental_health (flagged) | — (bỏ retrieval) | — (không gọi LLM) | 0 / 0 | $0 |

- Trả lời nutrition **né hải sản** (gợi ý gà), kèm disclaimer ✓. Mọi lượt ghi `prompt_logs` (token/cost/safety) ✓. Dữ liệu business **nguyên vẹn** sau khi đổi image pgvector (users=39). Secret NestJS↔FastAPI **khớp**, `RAG_SERVICE_URL=http://localhost:8001`. **Tổng chi phí demo ≈ $0.0004.**

---

## 9. Đánh giá

### Điểm mạnh
- **Tách dịch vụ sạch**, ranh giới dữ liệu rõ (SoT ở NestJS, AI-derived ở schema `rag`, business read-only).
- **An toàn ưu tiên**: pre/post-check + crisis short-circuit (không tốn token cho câu nguy hiểm).
- **Chi phí thấp** nhờ embedding local + `gpt-4o-mini` (~$0.0002/lượt).
- **Quan sát được**: log token/cost/intent/retrieval/safety per request → sẵn cho dashboard.
- **Không phá vỡ tích hợp cũ**: vẫn đúng contract `POST /chat` → `reply`.
- **Mở rộng dễ**: provider abstraction, proto gRPC & khung Kafka đã chừa sẵn.

### Hạn chế hiện tại
- **Intent bằng rule** (keyword) → dễ nhầm với câu mơ hồ/đa ý; chưa có query-rewrite/rerank.
- **Chunking xấp xỉ theo "từ"** (chưa dùng tokenizer thật) → ước lượng token chưa chuẩn.
- **Kho tri thức mỏng** (6 tài liệu seed); chưa embed thư viện `exercise`; chưa có nguồn nutrition/mental chất lượng cao.
- **Memory rolling đơn giản** (tóm tắt mỗi 6 lượt); chưa có trí nhớ dài hạn theo chủ đề.
- **Chưa có rate-limit/quota theo user**, chưa kiểm soát ngân sách OpenAI.
- **Chưa có eval tự động** (golden set, groundedness, hallucination, safety pass-rate).
- **Streaming/summary chưa nối tới mobile** (FastAPI có endpoint, NestJS gateway & UI chưa wire).
- **PII**: vẫn gửi `user_id` thô sang FastAPI (FastAPI hash trước khi lưu) — nên chuyển sang gửi `user_ref` hash từ NestJS.

---

## 10. Định hướng phát triển — còn thiếu gì cho **Mobile Production hoàn chỉnh**

### A. Phía AI service (P3–P6)
1. **Chất lượng RAG**: query-rewrite, hybrid search (vector + keyword), **rerank** (`bge-reranker`), context compression; chuyển **IVFFlat → HNSW** khi corpus lớn.
2. **Kho tri thức**: pipeline embed **thư viện `exercise`** (mô tả/form_tips) + nạp tài liệu nutrition/mental/FAQ chuẩn; versioning + reindex.
3. **Intent nâng cao**: phân loại bằng small-LLM khi rule không chắc; multi-intent.
4. **Event-driven (Kafka, P3)**: `nutrition.meal.logged`, `training.workout.completed`, … → tự cập nhật memory/summary/index; idempotency + DLT.
5. **gRPC (P4)** theo `proto/rag.proto` cho realtime nội bộ; giữ HTTP cho tương thích.
6. **Eval & an toàn (P5)**: golden dataset/intent, đo groundedness/hallucination/**safety pass-rate**/latency/cost; chạy regression khi đổi prompt/model/chunking; thêm OpenAI moderation.

### B. Phía Gateway (NestJS) — cần để mobile dùng được đầy đủ
7. **Endpoint mobile**: `POST /ai/chat/stream` (SSE proxy), `GET /ai/reports/daily|weekly` (đọc summary), chuẩn hoá response trả **structured** (`answer_markdown`, `sources`, `suggestions`, `safety`).
8. **Gửi `user_ref` hash** thay vì `user_id` thô; thêm **rate-limit/quota** theo user; **budget guard** cho OpenAI (ngắt khi vượt hạn mức ngày).
9. **Scheduler** phát `ai.daily_summary.requested` / `ai.weekly_report.requested`.

### C. Phía Mobile (Kotlin) — UX production
10. **Render chat phong phú**: Markdown, **chip "Nguồn"** (sources), **gợi ý nhanh** (suggestions), badge an toàn/disclaimer rõ ràng.
11. **Streaming UI**: nhận SSE từ `/ai/chat/stream`, hiển thị token dần (typing), nút dừng.
12. **Trạng thái**: loading/empty/error/timeout/offline; retry; lưu hội thoại offline (Room) + đồng bộ.
13. **An toàn người dùng**: khi `safety.flagged`, hiển thị banner hỗ trợ/đường dây nóng; chặn spam.
14. **Báo cáo AI**: màn "Tóm tắt ngày/Tuần" đọc từ `/ai/reports/*`.
15. **Phân tích & phản hồi**: nút 👍/👎 (ghi `prompt_logs.feedback`) để cải thiện.

### D. Vận hành / Production hardening
16. **Bảo mật**: secret rotation, mTLS nội bộ (hoặc gRPC + token), không log nội dung thô; rà soát PII.
17. **Triển khai**: tách dev/staging/prod, CI/CD (lint+test+eval+migrate), scale uvicorn workers, tách Kafka consumer thành process riêng, health/readiness probes.
18. **Monitoring/alert**: dashboard latency/cost/error/safety-fail/retrieval-empty; cảnh báo vượt ngân sách & DLT.
19. **Chi phí**: theo dõi `cost_usd` theo ngày/user; cân nhắc cache câu trả lời phổ biến; chọn model theo độ khó.
20. **Pháp lý/nội dung**: disclaimer y tế rõ ràng trong app, điều khoản sử dụng cho tư vấn sức khỏe.

---

## 11. Checklist production-readiness

- [x] RAG chat hoạt động (retrieve + generate) — chạy thật với OpenAI
- [x] Embedding local + pgvector trên DB thật (data giữ nguyên)
- [x] Safety pre/post + crisis short-circuit
- [x] Logging token/cost/intent/retrieval/safety
- [x] Memory rolling + daily/weekly summary (API)
- [x] SSE streaming (API)
- [x] Provider abstraction + degrade an toàn
- [x] Secret NestJS↔FastAPI khớp
- [ ] NestJS gateway `/ai/*` (stream + reports) + structured response
- [ ] Mobile UI: sources/suggestions/streaming/safety/feedback
- [ ] Gửi `user_ref` hash thay `user_id`; rate-limit + budget guard
- [ ] Embed thư viện exercise + kho tri thức chất lượng
- [ ] Eval tự động (golden set) + monitoring/alert
- [ ] Kafka (P3) / gRPC (P4) / rerank-HNSW (P5–P6)
- [ ] CI/CD, secrets management, mTLS, dashboards

---

## 12. Kết luận & khuyến nghị thứ tự ưu tiên

Nền tảng RAG đã **chạy thật, an toàn, chi phí thấp** và không phá vỡ hệ thống cũ. Để đạt **Mobile Production hoàn chỉnh**, ưu tiên theo thứ tự:

1. **Nối mobile end-to-end**: NestJS `/ai/*` (stream + reports + structured) → Mobile render sources/suggestions/streaming/safety + nút feedback. *(giá trị người dùng cao nhất, ít rủi ro)*
2. **Hardening bảo mật & chi phí**: gửi `user_ref` hash, rate-limit, budget guard, không log PII.
3. **Chất lượng tri thức & retrieval**: embed exercise + nạp tài liệu chuẩn; rerank/hybrid; intent bằng LLM.
4. **Eval + monitoring**: golden set, dashboards, alert — bắt buộc trước khi mở rộng người dùng.
5. **Sau cùng**: Kafka (P3), gRPC (P4), HNSW/Qdrant (P6).

_Tránh over-engineering sớm: chưa cần Qdrant/gRPC/Kafka cho lần ra mắt đầu; tập trung trải nghiệm chat mượt + an toàn + đo lường được._
