-- Run this once against the calories_tracker database to create a read-only role
-- for the RAG service to access user data without write privileges.
--
-- Usage:
--   psql -h localhost -p 5433 -U postgres -d calories_tracker -f scripts/create_rag_reader.sql

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rag_reader') THEN
    CREATE ROLE rag_reader WITH LOGIN PASSWORD 'ragpass' NOSUPERUSER NOCREATEDB NOCREATEROLE;
    RAISE NOTICE 'Role rag_reader created';
  ELSE
    RAISE NOTICE 'Role rag_reader already exists';
  END IF;
END
$$;

-- Grant read-only access to all current and future tables
GRANT CONNECT ON DATABASE calories_tracker TO rag_reader;
GRANT USAGE ON SCHEMA public TO rag_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO rag_reader;

-- Ensure future tables are also covered
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO rag_reader;
