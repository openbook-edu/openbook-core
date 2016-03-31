/*
  Using GIST index created index
 */

CREATE EXTENSION pg_trgm;
CREATE INDEX trgm_idx ON users USING gist (email gist_trgm_ops);

