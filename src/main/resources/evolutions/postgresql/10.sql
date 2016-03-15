CREATE TABLE media_tasks (
  task_id uuid PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
  media_type int
);