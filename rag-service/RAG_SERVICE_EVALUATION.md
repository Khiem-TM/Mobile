# Báo cáo đánh giá `rag-service` và hướng triển khai tiếp theo

Ngày đánh giá: 2026-06-06

Phạm vi kiểm tra:

- `rag-service/`: FastAPI RAG Core, ingestion, retrieval, memory, summary/report, schema `rag`.
- `backend/src/modules/chatbot/`: NestJS gateway, chat session/message, SSE proxy, UserContext.
- `backend/src/modules/support/rag/`: trigger embed khi dữ liệu người dùng thay đổi.
- `mobile/app/src/main/java/com/vitalai/.../coach`: UI chat streaming, source chips, disclaimer.
- `docker-compose.yml`: Postgres pgvector, Redis, rag-service.

## 1. Kết luận điều hành

`rag-service` hiện đã đạt mức MVP tốt: tách thành FastAPI service riêng, dùng PostgreSQL + pgvector, có provider abstraction cho LLM/embedding, chat đồng bộ và streaming, safety guardrail cơ bản, logging retrieval/prompt cho luồng đồng bộ, và đã được nối với NestJS/mobile qua module chatbot.

Tuy nhiên, nếu xét mục tiêu vận hành khi đã có dữ liệu thật, service chưa nên được xem là production RAG hoàn chỉnh. Các điểm cần xử lý trước khi mở rộng dữ liệu:

- `/embed/user/{user_id}` hiện vẫn là no-op. Backend đã trigger khi health profile/body metrics đổi, nhưng FastAPI chưa tạo index, summary hay memory mới.
- Luồng streaming chưa ghi token/cost/prompt log sau khi stream, chưa post-check nội dung sinh ra, và chưa refresh memory.
- Response structured của RAG (`sources`, `safety`, `usage`) bị mất ở luồng chat đồng bộ vì NestJS chỉ lấy `reply`; mobile reload history cũng mất `sources/disclaimer` vì `chat_messages` chỉ lưu role/content.
- Chưa có eval tự động cho retrieval/answer/safety, chưa có rate-limit/quota/budget guard, và readiness chưa phân biệt rõ chế độ stub/hash embedding với cấu hình production.
- Ingestion tài liệu mới chỉ insert thêm, chưa có idempotency theo checksum/source_id/version, nên dễ trùng chunks khi re-seed hoặc reindex.

Hướng triển khai khi có dữ liệu: không vector hóa toàn bộ dữ liệu cá nhân thô. Nên tách 2 pipeline:

1. Knowledge pipeline: tài liệu curated, FAQ, exercise library, guideline dinh dưỡng/tập luyện được chunk, embed, version và reindex vào `rag.rag_documents`/`rag.rag_chunks`.
2. User intelligence pipeline: dữ liệu cá nhân đi qua `UserContext`, daily/weekly summary, rolling memory và event-driven refresh; chỉ lưu AI-derived summary đã mask PII, không lưu raw profile/chat/meal log trong vector store.

## 2. Kiến trúc hiện tại

### `rag-service`

- FastAPI entrypoint: `rag-service/app/main.py`.
- Endpoint chính:
  - `POST /chat`: chat đồng bộ, trả `reply`, `intent`, `sources`, `safety`, `usage`.
  - `POST /chat/stream`: SSE `meta -> delta* -> done`.
  - `POST /embed/document`: nạp tài liệu vào vector DB.
  - `POST /embed/user/{id}`: trigger từ NestJS, hiện chỉ log và trả accepted.
  - `POST/GET /summary/daily`, `POST/GET /report/weekly`: sinh/đọc summary/report.
  - `/health`, `/ready`, `/metrics`.
- Pipeline chat đồng bộ:
  `safety pre-check -> intent rule -> embed query -> pgvector search -> memory -> prompt -> LLM -> safety post-check -> memory refresh -> retrieval_logs/prompt_logs`.
- Vector store:
  PostgreSQL + pgvector, schema `rag`, embedding `vector(768)`, metadata JSONB, IVFFlat cosine index.
- Providers:
  OpenAI/Gemini/stub cho LLM; local `intfloat/multilingual-e5-base` hoặc OpenAI cho embedding. Local embed có fallback hash cho dev.

### NestJS gateway

- `ChatbotService` quản lý session/message và gọi RAG bằng internal secret.
- `AiContextService` build `UserContext v1` gồm health profile, dashboard trong ngày, latest body và `user_ref` hash.
- `ChatbotController` đã có endpoint streaming `/chatbot/sessions/:id/messages/stream`.
- `RagEmbedService` và `ChatbotService.triggerUserEmbed` đã gọi `/embed/user/{id}` khi profile/body metrics thay đổi, nhưng FastAPI chưa xử lý thật.

### Mobile

- `ChatbotRepository.sendMessageStream` nhận SSE từ NestJS, fallback sang sync nếu stream lỗi trước token đầu tiên.
- `CoachViewModel` cập nhật message streaming, nhận `intent`, `sources`, `disclaimer`.
- `CoachScreen` render source chips và disclaimer cơ bản.

## 3. Đánh giá theo thành phần

| Thành phần | Hiện trạng | Đánh giá |
|---|---|---|
| Service boundary | FastAPI tách riêng, NestJS giữ business SoT | Tốt, đúng hướng để scale và bảo mật key |
| Auth nội bộ | `X-Internal-Secret` | Đủ cho dev/staging; production cần secret rotation/mTLS hoặc service mesh |
| UserContext | NestJS mask PII và có `user_ref` | Tốt, nhưng request vẫn gửi `user_id` thô sang RAG |
| Retrieval | Vector search pgvector + filter source theo intent | Tốt cho corpus nhỏ; thiếu hybrid, rerank, threshold, query rewrite |
| Ingestion document | Clean/chunk/embed/index | Có nền tảng, nhưng thiếu checksum/upsert/version cleanup |
| Ingestion user | Backend trigger có, FastAPI no-op | Chưa sẵn sàng khi dữ liệu user thay đổi liên tục |
| Safety | Rule pre-check crisis, post-check nutrition/disclaimer | Tốt cho MVP; cần moderation/eval và post-check đủ cho streaming |
| Streaming | End-to-end FastAPI -> NestJS -> mobile | Đã có; thiếu prompt/cost log, post-safety và memory refresh |
| Summary/report | Endpoint FastAPI đã có | Chưa thấy NestJS/mobile gateway đọc hiển thị daily/weekly report |
| Observability | Prometheus metrics + retrieval/prompt logs | Nền tảng tốt; streaming và stub/hash readiness còn thiếu |
| Mobile UX | Streaming, typing, sources, disclaimer | Đã vượt báo cáo cũ; còn thiếu Markdown rich, feedback, history metadata |
| Testing | Unit test intent/safety/chunking, NestJS SSE, mobile parser/viewmodel | Tốt cho luồng cơ bản; thiếu integration/eval với DB/vector/LLM |

## 4. Điểm mạnh

- Ranh giới dữ liệu hợp lý: NestJS là source-of-truth; RAG đọc schema `public` read-only và ghi AI-derived data vào schema `rag`.
- Pipeline RAG đồng bộ đã có đủ bước cần thiết cho MVP: safety, intent, retrieval, prompt, LLM, logs.
- Chi phí có thể thấp: local embedding, model mặc định `gpt-4o-mini`, cache query embedding bằng Redis.
- Tích hợp mobile streaming đã tồn tại, có fallback khi SSE lỗi trước khi nhận token.
- Schema `rag` đã đủ nền tảng cho knowledge base, memory, daily/weekly report, logs và idempotency event.
- Provider factory giúp dev không bị chặn khi thiếu key/dependency.

## 5. Hạn chế và rủi ro

### Rủi ro cần xử lý trước khi dùng dữ liệu thật

1. `embed_user` chưa làm gì.
   Backend đã trigger khi health profile/body metrics thay đổi, nhưng FastAPI chỉ log `embed_user_trigger`. Khi có dữ liệu, chatbot vẫn chỉ dựa vào `UserContext` tại thời điểm chat và knowledge seed, không có memory/index cập nhật theo sự kiện.

2. Streaming chưa đủ an toàn và quan sát được.
   Luồng `/chat/stream` không ghi `prompt_logs`, không có token/cost, không post-check output sinh ra, và không refresh memory. Nếu mobile ưu tiên streaming, telemetry chat sẽ lệch so với chat đồng bộ.

3. Contract còn gửi raw `user_id`.
   `AiContextService` đã tạo `user_ref`, proto gRPC cũng thiết kế `user_ref`, nhưng `ChatbotService.prepareChatRequest` vẫn gửi `user_id`, và FastAPI lại hash `req.user_id`. Nên đổi schema sang `user_ref` để RAG không nhận id thật.

4. Metadata RAG chưa được lưu cùng chat history.
   `chat_messages` chỉ lưu role/content. Sau khi reload, mobile mất `sources`, `intent`, `disclaimer`, `safety`, `usage`. Điều này ảnh hưởng UX, audit và feedback.

5. Chưa có eval gate.
   Chưa có golden dataset, retrieval hit@k, groundedness, hallucination, safety pass-rate, latency/cost regression. Không nên nạp corpus lớn hoặc đổi prompt/model mà không có eval.

### Rủi ro tiếp theo

- Intent rule dễ nhầm với câu hỏi đa ý hoặc tiếng Việt không chứa keyword.
- Chunking tính bằng số từ, chưa dùng tokenizer; có thể vượt context hoặc cắt sai ý.
- Search chưa có threshold, nên có thể đưa chunk kém liên quan vào prompt.
- IVFFlat cần `ANALYZE` và tuning lists/probes; khi corpus lớn nên cân nhắc HNSW.
- Hash embedding fallback chỉ phù hợp dev; readiness hiện vẫn `ready` nếu DB ok, kể cả LLM stub/hash embedder.
- `conversation_memories.updated_at` không được update trong upsert, làm khó audit freshness.
- `prompt_logs.feedback` đã có cột nhưng chưa có endpoint/UI ghi feedback.
- `BusinessRepo.lookup_food` và `get_exercises_for_embedding` đã có, nhưng chưa được wire vào ingestion/retrieval.

## 6. Hướng triển khai khi đã có dữ liệu

### 6.1. Phân loại dữ liệu

| Nhóm dữ liệu | Cách dùng khuyến nghị | Lý do |
|---|---|---|
| Guideline nutrition/workout/mental/FAQ | Chunk + embed vào `rag_chunks` | Knowledge ổn định, cần source và version |
| Exercise library | Embed description/instructions/form_tips; metadata `exercise_id`, muscle_group, difficulty, type | Phù hợp semantic retrieval cho bài tập và form tips |
| Food database | Ưu tiên structured lookup theo tên/alias/nutrition facts; chỉ embed FAQ/guideline | Calo/macro cần chính xác, không nên lấy bằng semantic chunk |
| Health profile/body metrics/dashboard | Gửi qua `UserContext`, summary/report, rolling memory | Dữ liệu cá nhân thay đổi liên tục, không nên vector hóa raw |
| Meal/training events | Event-driven cập nhật summary/cache/trend | Tốt cho personalization, không cần raw chunk |
| Chat history | NestJS SoT; RAG chỉ lưu rolling summary | Giảm PII và chi phí, tránh prompt quá dài |
| Feedback user | Ghi vào prompt/retrieval logs và eval set | Cần cho cải thiện prompt/retrieval |

### 6.2. Phase 1 - Chuẩn hóa contract và nền tảng dữ liệu

- Đổi `ChatRequest` từ `user_id` sang `user_ref`, hoặc chấp nhận cả hai trong giai đoạn migrate; NestJS gửi `AiContextService.userRef(userId)`.
- Cập nhật `RagOrchestrator` dùng `req.user_ref` trực tiếp, không hash lại.
- Thêm unique/idempotency cho knowledge: `source`, `uri`/`external_id`, `version`, `checksum`.
- Khi checksum không đổi thì bỏ qua; khi version mới thì xóa/cập nhật chunks cũ theo document/version.
- Thêm migration Alembic thay vì chỉ sửa `init_db.sql`.
- Ready check phải báo `degraded` nếu LLM là stub hoặc embedder là hash fallback trong non-dev.
- Chuẩn hóa metadata chunk: `source`, `kind`, `source_id`, `lang`, `version`, `quality`, `owner`, `updated_at`.

### 6.3. Phase 2 - Nạp knowledge base thật

- Seed tài liệu curated:
  - Nutrition principles, calories/macros, allergies, hydration, safe weight loss.
  - Workout safety, beginner plans, warm-up, injury prevention, form guidance.
  - Mental support scope, stress/sleep/motivation, crisis handling.
  - App FAQ: log meal, scan food, water/steps, body metrics, workout tracking.
- Wire exercise ingestion từ `BusinessRepo.get_exercises_for_embedding()`:
  - Content gồm `name`, `description`, `instructions`, `form_tips`.
  - Metadata gồm `exercise_id`, `muscle_group`, `difficulty_level`, `met_value`, `exercise_type`.
  - Source nên là `workout`, kind là `exercise`.
- Không embed raw food table trước. Nếu cần, thêm tool structured lookup trong retrieval/prompt để chatbot tra calo/macro chính xác hơn.
- Tạo script/endpoint admin-only để reindex theo batch: dry-run, batch embed, commit theo batch, log ingest status.

### 6.4. Phase 3 - Biến `/embed/user` thành pipeline thật

- Thay no-op bằng job event-driven:
  - `profile.updated`: refresh UserContext snapshot/cache nếu cần.
  - `body_metric.upserted`: cập nhật latest/trend summary.
  - `meal.logged`, `water.updated`, `training.completed`: cập nhật daily summary input hoặc schedule summary.
- Dùng `processed_events` cho idempotency.
- Không lưu raw PII/raw health rows vào vector DB. Chỉ lưu:
  - rolling conversation memory.
  - daily summary.
  - weekly report.
  - derived facts an toàn nếu cần, ví dụ `goal=weight_loss`, `allergies=[...]`, `prefers_home_workout`.
- Nên để NestJS là nơi build payload đầy đủ; RAG chỉ xử lý AI-derived output.

### 6.5. Phase 4 - Nâng chất lượng retrieval

Chỉ làm sau khi corpus có đủ kích thước và có eval.

- Thêm query rewrite cho câu hỏi dài hoặc multi-intent.
- Hybrid search: vector + keyword/FTS, đặc biệt cho FAQ, tên bài tập, tên món.
- Thêm similarity threshold; nếu hit kém thì nói thiếu nguồn thay vì chèn chunk không liên quan.
- Rerank top 20 -> top 5 bằng reranker nhẹ.
- Metadata filter theo `source`, `kind`, `lang`, `quality`, `difficulty`, `goal_type`.
- Context compression để giảm token khi chunks dài.
- Khi chunks tăng lớn: tuning IVFFlat `lists/probes`, chạy `ANALYZE`, sau đó cân nhắc HNSW. Chỉ cần Qdrant/Weaviate nếu PostgreSQL không đáp ứng latency/ops.

### 6.6. Phase 5 - Hoàn thiện gateway/mobile

- Sync API NestJS nên trả structured response, không chỉ `ChatMessage` content:
  `answer_markdown`, `intent`, `sources`, `safety`, `usage`, `suggestions`.
- Lưu metadata assistant message: sources, intent, safety, disclaimer, request_id. Sau reload mobile vẫn render đúng source/disclaimer.
- Thêm endpoint daily/weekly report qua NestJS:
  - `GET /chatbot/reports/daily?date=...`
  - `GET /chatbot/reports/weekly?week_start=...`
- Thêm feedback: mobile like/dislike/report issue; backend cập nhật `prompt_logs.feedback` theo `request_id`.
- Mobile: render Markdown, source detail bottom sheet, safety banner riêng, stop generation/cancel stream nếu cần.

### 6.7. Phase 6 - Eval, monitoring và production hardening

- Tạo golden dataset tiếng Việt theo intent: nutrition, workout, body_metrics, mental_health, app_help, smalltalk.
- Include case allergies, unsafe dieting, crisis, missing data, multi-intent.
- Metrics bắt buộc: retrieval hit@k/MRR, groundedness/source citation, hallucination rate, safety pass-rate, p50/p95 latency, stream first-token, token/cost per request/user/day, retrieval empty rate.
- CI gate: unit tests, integration test với test Postgres pgvector, eval regression khi đổi prompt/model/chunking.
- Ops: rate-limit theo user/IP, daily budget guard cho OpenAI, secret rotation, không log raw message/user_id trong sink ngoài, dashboard error/latency/cost/safety/retrieval-empty, alert nếu dùng stub/hash trong staging/prod.

## 7. Thứ tự ưu tiên đề xuất

1. Sửa contract privacy: RAG nhận `user_ref`, không nhận raw `user_id`.
2. Sửa streaming parity: log prompt/cost, post-check output, refresh memory hoặc ghi rõ không refresh.
3. Làm ingestion idempotent cho document: checksum/version/delete old chunks.
4. Wire exercise ingestion từ DB business và nạp FAQ/guideline curated.
5. Biến `/embed/user` thành event/summary refresh thật, tối thiểu xử lý profile/body metrics update.
6. Lưu metadata RAG vào chat history và trả structured response cho sync.
7. Thêm report endpoints qua NestJS/mobile.
8. Tạo golden eval dataset và chạy eval trước/sau khi nạp corpus.
9. Thêm rate-limit, budget guard, readiness fail/degraded cho stub/hash.
10. Sau khi có số liệu retrieval/latency mới tính hybrid/rerank/HNSW.

## 8. Tiêu chí go-live

- Production không dùng StubLLM/HashEmbed.
- Chat sync và streaming đều có prompt/retrieval logs đầy đủ.
- Crisis input được block trước LLM và có regression test.
- Allergy trong UserContext được tôn trọng trong answer eval.
- Retrieval empty rate được theo dõi; câu trả lời khi thiếu nguồn không bịa.
- p95 chat sync và stream first-token có dashboard riêng.
- Cost theo ngày/user có budget guard.
- Mobile reload chat vẫn còn sources/disclaimer của assistant messages.
- Knowledge ingestion có checksum/version và có thể reindex không trùng lặp.
- Golden eval pass trước mỗi lần đổi prompt/model/corpus lớn.

## 9. Kết luận

Nên tiếp tục phát triển trên kiến trúc hiện tại. Không nên thay vector DB, gRPC hay Kafka quá sớm. Việc có giá trị nhất khi đã có dữ liệu là đưa dữ liệu vào đúng đường: knowledge curated/exercise vào vector store, còn user data vào UserContext/summary/memory đã mask. Sau đó mới tối ưu retrieval bằng hybrid/rerank/HNSW dựa trên eval và telemetry thật.

