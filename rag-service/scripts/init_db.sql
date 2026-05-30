-- ============================================================================
-- VitalAI RAG — bootstrap schema `rag` + pgvector + read-only role `rag_reader`.
-- Chạy 1 lần với quyền superuser trên DB `calories_tracker`.
--   psql "postgresql://postgres:123456@localhost:5433/calories_tracker" -f scripts/init_db.sql
-- (Khớp plan mục 6. embedding vector(768) = intfloat/multilingual-e5-base.)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE SCHEMA IF NOT EXISTS rag;

-- ── Tables ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS rag.rag_documents (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  source      varchar(32) NOT NULL,
  title       text NOT NULL,
  lang        varchar(8) DEFAULT 'vi',
  version     int DEFAULT 1,
  quality     varchar(16) DEFAULT 'curated',
  uri         text,
  checksum    text,
  created_at  timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS rag.rag_chunks (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id  uuid REFERENCES rag.rag_documents(id) ON DELETE CASCADE,
  chunk_index  int NOT NULL,
  content      text NOT NULL,
  token_count  int,
  embedding    vector(768) NOT NULL,
  metadata     jsonb DEFAULT '{}',
  created_at   timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS rag.conversation_memories (
  user_ref      varchar(64) PRIMARY KEY,
  summary       text,
  salient_facts jsonb DEFAULT '[]',
  updated_at    timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS rag.user_ai_daily_summaries (
  user_ref   varchar(64),
  date       date,
  summary    text,
  metrics    jsonb DEFAULT '{}',
  created_at timestamptz DEFAULT now(),
  PRIMARY KEY (user_ref, date)
);

CREATE TABLE IF NOT EXISTS rag.user_ai_weekly_reports (
  user_ref   varchar(64),
  week_start date,
  report     jsonb DEFAULT '{}',
  created_at timestamptz DEFAULT now(),
  PRIMARY KEY (user_ref, week_start)
);

CREATE TABLE IF NOT EXISTS rag.retrieval_logs (
  id         bigserial PRIMARY KEY,
  request_id uuid,
  user_ref   varchar(64),
  intent     varchar(32),
  query      text,
  chunk_ids  uuid[],
  scores     real[],
  top_k      int,
  latency_ms int,
  created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS rag.prompt_logs (
  id             bigserial PRIMARY KEY,
  request_id     uuid,
  user_ref       varchar(64),
  intent         varchar(32),
  prompt_version varchar(16),
  model          varchar(40),
  tokens_in      int,
  tokens_out     int,
  cost_usd       numeric(10,6),
  safety_result  varchar(16),
  feedback       smallint,
  created_at     timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS rag.processed_events (
  event_id     uuid PRIMARY KEY,
  processed_at timestamptz DEFAULT now()
);

-- ── Indexes ─────────────────────────────────────────────────────────────────
-- MVP: IVFFlat (đổi sang HNSW khi corpus lớn — xem plan mục 6).
CREATE INDEX IF NOT EXISTS idx_rag_chunks_embedding
  ON rag.rag_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_rag_chunks_metadata
  ON rag.rag_chunks USING gin (metadata);
CREATE INDEX IF NOT EXISTS idx_prompt_logs_created ON rag.prompt_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_retrieval_logs_created ON rag.retrieval_logs (created_at);

-- ── Read-only role cho FastAPI (business data) + quyền ghi schema rag ────────
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rag_reader') THEN
    CREATE ROLE rag_reader LOGIN PASSWORD 'ragpass';
  END IF;
END$$;

-- Business data: chỉ SELECT (public schema)
GRANT USAGE ON SCHEMA public TO rag_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO rag_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO rag_reader;

-- RAG data: toàn quyền trong schema rag
GRANT USAGE, CREATE ON SCHEMA rag TO rag_reader;
GRANT ALL ON ALL TABLES IN SCHEMA rag TO rag_reader;
GRANT ALL ON ALL SEQUENCES IN SCHEMA rag TO rag_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA rag GRANT ALL ON TABLES TO rag_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA rag GRANT ALL ON SEQUENCES TO rag_reader;
