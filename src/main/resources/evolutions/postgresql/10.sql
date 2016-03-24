CREATE TABLE media_tasks (
  task_id uuid PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
  media_type int
);

CREATE TABLE media_work (
  work_id uuid REFERENCES work(id) ON DELETE CASCADE,
  file_data jsonb,
  PRIMARY KEY (work_id)
);

CREATE TABLE media_work_data (
  work_id uuid REFERENCES media_work(work_id) ON DELETE CASCADE,
  version bigint,
  file_data jsonb,
  created_at timestamp with time zone,
  PRIMARY KEY (work_id, version)
);