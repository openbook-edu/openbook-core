CREATE TABLE users (
  id bytea PRIMARY KEY,
  version bigint,
  email text UNIQUE,
  username text UNIQUE,
  password_hash text,
  givenname text,
  surname text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE roles (
  id bytea PRIMARY KEY,
  version bigint,
  name text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE users_roles (
  user_id bytea REFERENCES users(id) ON DELETE CASCADE,
  role_id bytea REFERENCES roles(id) ON DELETE CASCADE,
  created_at timestamp with time zone,
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE courses (
  id bytea PRIMARY KEY,
  version bigint,
  teacher_id bytea NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  name text,
  color integer,
  slug text,
  chat_enabled boolean DEFAULT true,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE users_courses (
  user_id bytea REFERENCES users(id) ON DELETE CASCADE,
  course_id bytea REFERENCES courses(id) ON DELETE CASCADE,
  created_at timestamp with time zone,
  PRIMARY KEY (user_id, course_id)
);

CREATE TABLE course_schedules (
  id bytea PRIMARY KEY,
  course_id bytea NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  version bigint,
  day timestamp with time zone,
  start_time timestamp with time zone,
  end_time timestamp with time zone,
  description text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE course_schedule_exceptions (
  id bytea PRIMARY KEY,
  user_id bytea NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  course_id bytea NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
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
  course_id bytea NOT NULL REFERENCES courses(id) ON DELETE RESTRICT,
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
  project_id bytea NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  name text,
  enabled boolean DEFAULT true,
  position int,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

/*
  All editable multi-line text areas will be considered "documents" and stored with a single mechanism.

  The "documents" table is like the "master record" for each document. It contains data such as
  the current version of the document, its latest computed contents, and the latest computed checksum of its
  contents. It also contains the times that it was created and last updated.
*/

CREATE TABLE documents (
  id bytea PRIMARY KEY,
  version bigint,
  owner_id bytea NOT NULL REFERENCES users(id),
  title text,
  plaintext text,
  delta json,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

/*
  Each document revision is stored in a separate revision table.

  The revisions table stores the complete revision history of a document. It is keyed by both the document ID and
  version number. Each row stores the time it was created and the revision details in a json representation with the
  following format:

  [{"p": 123, "t": "i", "chars": "blahblah"}]

  Where "p" is the position of the edit, "t" is either "i" for insert or "d" for delete, and "chars" is the text to be
  inserted or deleted. It can consist of an array of json values.
*/

CREATE TABLE document_revisions (
  document_id bytea NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  version bigint,
  author_id bytea NOT NULL REFERENCES users(id),
  delta json,
  created_at timestamp with time zone
);

CREATE TABLE tasks (
  id bytea PRIMARY KEY,
  version bigint,
  part_id bytea NOT NULL REFERENCES parts(id) ON DELETE CASCADE,
  dependency_id bytea REFERENCES tasks(id) ON DELETE RESTRICT,
  name text,
  description text,
  position int,
  task_type int,
  notes_allowed boolean DEFAULT true,
  response_title text,
  notes_title text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE long_answer_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE short_answer_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
  max_length int
);

CREATE TABLE multiple_choice_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
  choices text[],
  answers int[],
  allow_multiple boolean DEFAULT false,
  randomize boolean DEFAULT true
);

CREATE TABLE ordering_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
  elements text[],
  answers int[],
  randomize boolean DEFAULT true
);

CREATE TABLE matching_tasks (
  task_id bytea PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
  elements_left text[],
  elements_right text[],
  answers int[][2],
  randomize boolean DEFAULT true
);

CREATE TABLE task_feedbacks (
  task_id    bytea REFERENCES tasks(id) ON DELETE RESTRICT,
  student_id bytea REFERENCES users(id) ON DELETE CASCADE,
  document_id bytea REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (task_id, student_id)
);

CREATE TABLE task_notes (
  user_id bytea REFERENCES users(id) ON DELETE CASCADE,
  task_id bytea REFERENCES tasks(id) ON DELETE RESTRICT,
  document_id bytea REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (user_id, task_id)
);


CREATE TABLE components (
  id bytea PRIMARY KEY,
  version bigint,
  owner_id bytea REFERENCES users(id) ON DELETE CASCADE,
  title text,
  questions text,
  things_to_think_about text,
  type text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE parts_components (
  component_id bytea REFERENCES components(id) ON DELETE CASCADE,
  part_id bytea REFERENCES parts(id) ON DELETE CASCADE,
  created_at timestamp with time zone,
  PRIMARY KEY (component_id, part_id)
);

CREATE TABLE text_components (
  component_id bytea PRIMARY KEY REFERENCES components(id) ON DELETE CASCADE,
  content text
);

CREATE TABLE video_components (
  component_id bytea PRIMARY KEY REFERENCES components(id) ON DELETE CASCADE,
  vimeo_id text,
  width int,
  height int
);

CREATE TABLE audio_components (
  component_id bytea PRIMARY KEY REFERENCES components(id) ON DELETE CASCADE,
  soundcloud_id text
);

CREATE TABLE component_notes (
  user_id bytea REFERENCES users(id) ON DELETE CASCADE,
  component_id bytea REFERENCES components(id) ON DELETE RESTRICT,
  document_id bytea REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (user_id, component_id, document_id)
);

/*
  Master records for student work.
*/

CREATE TABLE work (
  id bytea PRIMARY KEY,
  user_id bytea NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  task_id bytea NOT NULL REFERENCES tasks(id) ON DELETE RESTRICT,
  version bigint,
  is_complete boolean DEFAULT false,
  work_type int,
  created_at timestamp with time zone,
  updated_at timestamp with time zone,
  UNIQUE (user_id, task_id)
);

/*
  Long Answer and Short Answer work just are versioned using the OT document system. Thus
  their tables merely hold pointers to Documents.
*/

CREATE TABLE long_answer_work (
  work_id bytea REFERENCES work(id) ON DELETE CASCADE,
  document_id bytea REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (work_id, document_id)
);

CREATE TABLE short_answer_work (
  work_id bytea REFERENCES work(id) ON DELETE CASCADE,
  document_id bytea REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (work_id, document_id)
);

/*
  Multiple choice, ordering and matching are versioned using their own tables.
*/

CREATE TABLE multiple_choice_work (
  work_id bytea REFERENCES work(id) ON DELETE CASCADE,
  version bigint,
  response int[],
  created_at timestamp with time zone,
  PRIMARY KEY (work_id, version)
);

CREATE TABLE ordering_work (
  work_id bytea REFERENCES work(id) ON DELETE CASCADE,
  version bigint,
  response int[],
  created_at timestamp with time zone,
  PRIMARY KEY (work_id, version)
);

CREATE TABLE matching_work (
  work_id bytea REFERENCES work(id) ON DELETE CASCADE,
  version bigint,
  response int[][2],
  created_at timestamp with time zone,
  PRIMARY KEY (work_id, version)
);

CREATE TABLE journal (
  id bytea PRIMARY KEY,
  version bigint,
  user_id bytea REFERENCES users(id) ON DELETE CASCADE,
  project_id bytea REFERENCES projects(id) ON DELETE CASCADE,
  entry_type text,
  item text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

/* YEAR 2014 */
CREATE TABLE journal_201401 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-01-01' AND '2014-01-31')
) INHERITS (journal);

CREATE TABLE journal_201402 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-02-01' AND '2014-02-28')
) INHERITS (journal);

CREATE TABLE journal_201403 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-03-01' AND '2014-03-31')
) INHERITS (journal);

CREATE TABLE journal_201404 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-04-01' AND '2014-04-30')
) INHERITS (journal);

CREATE TABLE journal_201405 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-05-01' AND '2014-05-31')
) INHERITS (journal);

CREATE TABLE journal_201406 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-06-01' AND '2014-06-30')
) INHERITS (journal);

CREATE TABLE journal_201407 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-07-01' AND '2014-07-31')
) INHERITS (journal);

CREATE TABLE journal_201408 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-08-01' AND '2014-08-31')
) INHERITS (journal);

CREATE TABLE journal_201409 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-09-01' AND '2014-09-30')
) INHERITS (journal);

CREATE TABLE journal_201410 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-10-01' AND '2014-10-31')
) INHERITS (journal);

CREATE TABLE journal_201411 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-11-01' AND '2014-11-30')
) INHERITS (journal);

CREATE TABLE journal_201412 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-12-01' AND '2014-12-31')
) INHERITS (journal);

