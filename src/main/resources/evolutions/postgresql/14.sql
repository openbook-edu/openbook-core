ALTER TABLE projects ADD COLUMN parent_id uuid REFERENCES projects(id);
ALTER TABLE projects ADD COLUMN is_master boolean DEFAULT FALSE;