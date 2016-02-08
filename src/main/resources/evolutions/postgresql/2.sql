CREATE TABLE project_notes (
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  project_id uuid REFERENCES projects(id) ON DELETE RESTRICT,
  document_id uuid REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (user_id, project_id)
);
ALTER TABLE project_notes OWNER TO krispii_dev