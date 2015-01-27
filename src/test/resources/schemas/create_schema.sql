CREATE TABLE users (
  /* You can use just PRIMARY KEY because it is a combination of UNIQUE and NOT NULL*/
  id bytea,
  version bigint,
  email text,
  username text,
  password_hash text,
  givenname text,
  surname text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone,
  PRIMARY KEY (id, username)
);

CREATE TABLE roles (
  id bytea PRIMARY KEY,
  version bigint,
  name text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE users_roles (
  user_id bytea REFERENCES users(id),
  role_id bytea REFERENCES roles(id),
  created_at timestamp with time zone,
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE classes (
  id bytea PRIMARY KEY,
  version bigint,
  teacher_id bytea REFERENCES users(id),
  name text,
  color integer,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE users_classes (
  user_id bytea REFERENCES users(id),
  class_id bytea REFERENCES classes(id),
  created_at timestamp with time zone,
  PRIMARY KEY (user_id, class_id)
);

CREATE TABLE schedules (
  id bytea PRIMARY KEY,
  class_id bytea NOT NULL REFERENCES classes(id),
  version bigint,
  start_time timestamp with time zone,
  length int,
  reason text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE schedule_exceptions (
  id bytea PRIMARY KEY,
  user_id bytea NOT NULL REFERENCES users(id),
  class_id bytea NOT NULL REFERENCES classes(id),
  version bigint,
  day timestamp with time zone,
  start_time timestamp with time zone,
  end_time timestamp with time zone,
  reason text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE projects (
  id bytea PRIMARY KEY,
  class_id bytea NOT NULL REFERENCES classes(id),
  version bigint,
  name text,
  slug text,
  description text,
  availability text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE parts (
  id bytea PRIMARY KEY,
  version bigint,
  project_id bytea NOT NULL REFERENCES projects(id),
  name text,
  enabled boolean DEFAULT true,
  position int,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE documents (
  id bytea PRIMARY KEY,
  version bigint,
  owner_id bytea REFERENCES users(id),
  title text,
  plaintext text,
  delta json,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE document_revisions (
  document_id bytea REFERENCES documents(id),
  version bigint,
  author_id bytea REFERENCES users(id),
  delta json,
  created_at timestamp with time zone
);

CREATE TABLE tasks (
  id bytea PRIMARY KEY,
  version bigint,
  part_id bytea NOT NULL REFERENCES parts(id),
  dependency_id bytea REFERENCES tasks(id),
  name text,
  description text,
  position int,
  task_type int,
  notes_allowed boolean DEFAULT true,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE long_answer_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id)
);

CREATE TABLE short_answer_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id),
  max_length int
);

CREATE TABLE multiple_choice_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id),
  choices text[],
  answers int[],
  allow_multiple boolean DEFAULT false,
  randomize boolean DEFAULT true
);

CREATE TABLE ordering_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id),
  choices text[],
  answers int[],
  randomize boolean DEFAULT true
);

CREATE TABLE matching_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id),
  choices_left text[],
  choices_right text[],
  answers int[][2],
  randomize boolean DEFAULT true
);

CREATE TABLE task_feedbacks (
  task_id    bytea NOT NULL REFERENCES tasks(id),
  student_id bytea NOT NULL REFERENCES users(id),
  document_id bytea REFERENCES documents(id),
  PRIMARY KEY (task_id, student_id, document_id)
);

CREATE TABLE task_notes (
  user_id bytea REFERENCES users(id),
  task_id bytea REFERENCES tasks(id),
  document_id bytea REFERENCES documents(id),
  PRIMARY KEY (user_id, task_id, document_id)
);

CREATE TABLE components (
  id bytea PRIMARY KEY,
  version bigint,
  task_id bytea REFERENCES tasks(id),
  title text,
  questions text,
  things_to_think_about text,
  type text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE audio_components (
  component_id bytea PRIMARY KEY REFERENCES components(id),
  soundcloud_id text
);

CREATE TABLE text_components (
  component_id bytea PRIMARY KEY REFERENCES components(id),
  content text
);

CREATE TABLE video_components (
  component_id bytea PRIMARY KEY REFERENCES components(id),
  vimeo_id text,
  width int,
  height int
);

CREATE TABLE component_notes (
  user_id bytea REFERENCES users(id),
  component_id bytea REFERENCES components(id),
  document_id bytea REFERENCES documents(id),
  PRIMARY KEY (user_id, component_id, document_id)
);

CREATE TABLE work (
  id bytea PRIMARY KEY,
  user_id bytea REFERENCES users(id),
  task_id bytea REFERENCES tasks(id),
  version bigint,
  is_complete boolean,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
  /* PRIMARY KEY (id, user_id, task_id, version) */
);

CREATE TABLE long_answer_work (
  work_id bytea REFERENCES work(id),
  document_id bytea REFERENCES documents(id),
  PRIMARY KEY (work_id, document_id)
);

CREATE TABLE short_answer_work (
  work_id bytea REFERENCES work(id),
  document_id bytea REFERENCES documents(id),
  PRIMARY KEY (work_id, document_id)
);

CREATE TABLE multiple_choice_work (
  user_id bytea REFERENCES users(id),
  task_id bytea REFERENCES tasks(id),
  version bigint,
  response int[],
  PRIMARY KEY (user_id, task_id, version)
);

CREATE TABLE ordering_work (
  user_id bytea REFERENCES users(id),
  task_id bytea REFERENCES tasks(id),
  version bigint,
  response int[],
  PRIMARY KEY (user_id, task_id, version)
);

CREATE TABLE matching_work (
  user_id bytea REFERENCES users(id),
  task_id bytea REFERENCES tasks(id),
  version bigint,
  response int[][2],
  PRIMARY KEY (user_id, task_id, version)
);

/*
CREATE TABLE courses (
  id bytea NOT NULL PRIMARY KEY,
  version bigint,
  name text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE project_templates (
  id bytea NOT NULL PRIMARY KEY,
  version bigint,
  name text,
  slug text,
  description text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE activity_log (
  id bytea NOT NULL PRIMARY KEY,
  version bigint,
  user_id bytea REFERENCES users(id),
  message text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone,
  status int
);

CREATE TABLE components_parts (
  component_id bytea REFERENCES components(id),
  part_id bytea REFERENCES parts(id),
  created_at timestamp with time zone,
  PRIMARY KEY (component_id, part_id)
);

CREATE TABLE class_schedules (
  id bytea NOT NULL PRIMARY KEY,
  version bigint,
  class_id bytea NOT NULL REFERENCES classes(id),
  day timestamp with time zone,
  start_time timestamp with time zone,
  end_time timestamp with time zone,
  description text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE users_classes (
  user_id bytea REFERENCES users(id),
  class_id bytea REFERENCES classes(id),
  created_at timestamp with time zone,
  PRIMARY KEY (user_id, class_id)
);

CREATE TABLE classes_projects (
  project_id bytea REFERENCES projects(id),
  class_id bytea REFERENCES classes(id),
  created_at timestamp with time zone,
  PRIMARY KEY (project_id, class_id)
);

CREATE TABLE scheduled_classes_parts (
  class_id bytea REFERENCES classes(id),
  part_id bytea REFERENCES parts(id),
  active boolean,
  created_at timestamp with time zone,
  PRIMARY KEY (class_id, part_id)
);

CREATE TABLE student_responses (
  user_id bytea REFERENCES users(id),
  task_id bytea REFERENCES tasks(id),
  revision bigint,
  version bigint,
  response text,
  is_complete boolean,
  created_at timestamp with time zone,
  updated_at timestamp with time zone,
  status int,
  PRIMARY KEY (user_id, task_id, revision)
);

CREATE TABLE logbook (
  id bytea NOT NULL PRIMARY KEY,
  version bigint,
  remote_address text,
  request_uri text,
  user_agent text,
  user_id bytea,
  message text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone,
  status int
);
*/
