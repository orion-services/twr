-- Runs automatically on first container start (docker-entrypoint-initdb.d),
-- i.e. only when the postgres_data volume is empty/freshly created. In dev
-- this extension is created automatically by Quarkus Dev Services/Testcontainers;
-- in prod (plain Docker Compose) nothing does that for us, so the app fails
-- with `type "vector" does not exist` on first request unless we do it here.
CREATE EXTENSION IF NOT EXISTS vector;
