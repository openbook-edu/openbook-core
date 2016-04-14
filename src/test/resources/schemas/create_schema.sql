CREATE TABLE users (
  id uuid PRIMARY KEY,
  version bigint,
  email text UNIQUE,
  username text UNIQUE,
  password_hash text,
  givenname text,
  surname text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone,
  is_deleted boolean DEFAULT FALSE
);

CREATE TABLE roles (
  id uuid PRIMARY KEY,
  version bigint,
  name text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE users_roles (
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  role_id uuid REFERENCES roles(id) ON DELETE CASCADE,
  created_at timestamp with time zone,
  PRIMARY KEY (user_id, role_id)
);


CREATE TABLE user_tokens (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  nonce text UNIQUE,
  token_type text,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);


CREATE TABLE courses (
  id uuid PRIMARY KEY,
  version bigint,
  teacher_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  name text,
  color integer,
  slug text UNIQUE,
  enabled boolean DEFAULT true,
  is_deleted boolean DEFAULT false,
  scheduling_enabled boolean DEFAULT false,
  chat_enabled boolean DEFAULT true,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE users_courses (
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  course_id uuid REFERENCES courses(id) ON DELETE CASCADE,
  created_at timestamp with time zone,
  PRIMARY KEY (user_id, course_id)
);

CREATE TABLE course_schedules (
  id uuid PRIMARY KEY,
  course_id uuid NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  version bigint,
  day date,
  start_time time,
  end_time time,
  description text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE course_schedule_exceptions (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  course_id uuid NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  version bigint,
  day date,
  start_time time,
  end_time time,
  reason text,
  block boolean DEFAULT false,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE projects (
  id uuid PRIMARY KEY,
  course_id uuid NOT NULL REFERENCES courses(id) ON DELETE RESTRICT,
  version bigint,
  name text,
  slug text UNIQUE,
  description text,
  availability text,
  parent_id uuid,
  is_master boolean DEFAULT false,
  enabled boolean DEFAULT false,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE parts (
  id uuid PRIMARY KEY,
  version bigint,
  project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
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
  id uuid PRIMARY KEY,
  version bigint,
  owner_id uuid NOT NULL REFERENCES users(id),
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
  document_id uuid NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  version bigint,
  author_id uuid NOT NULL REFERENCES users(id),
  delta json,
  created_at timestamp with time zone,
  PRIMARY KEY (document_id, version)
);

CREATE TABLE tasks (
  id uuid PRIMARY KEY,
  version bigint,
  part_id uuid NOT NULL REFERENCES parts(id) ON DELETE CASCADE,
  name text,
  description text,
  position int,
  task_type int,
  notes_allowed boolean DEFAULT true,
  response_title text,
  notes_title text,
  help_text text,
  max_grade text DEFAULT '0',
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE document_tasks (
  task_id uuid PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
  dependency_id uuid REFERENCES tasks(id) ON DELETE RESTRICT
);

CREATE TABLE question_tasks (
  task_id uuid PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
  questions jsonb
);

CREATE TABLE media_tasks (
  task_id uuid PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
  media_type int
);

CREATE TABLE task_feedbacks (
  task_id    uuid REFERENCES tasks(id) ON DELETE RESTRICT,
  student_id uuid REFERENCES users(id) ON DELETE CASCADE,
  document_id uuid REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (task_id, student_id)
);

CREATE TABLE task_notes (
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  task_id uuid REFERENCES tasks(id) ON DELETE RESTRICT,
  document_id uuid REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (user_id, task_id)
);


CREATE TABLE components (
  id uuid PRIMARY KEY,
  version bigint,
  owner_id uuid REFERENCES users(id) ON DELETE CASCADE,
  title text,
  questions text,
  things_to_think_about text,
  type text,
  ord integer DEFAULT 0,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE parts_components (
  component_id uuid REFERENCES components(id) ON DELETE CASCADE,
  part_id uuid REFERENCES parts(id) ON DELETE CASCADE,
  created_at timestamp with time zone,
  PRIMARY KEY (component_id, part_id)
);

CREATE TABLE text_components (
  component_id uuid PRIMARY KEY REFERENCES components(id) ON DELETE CASCADE,
  content text
);

CREATE TABLE generic_html_components (
   component_id uuid PRIMARY KEY REFERENCES components(id) ON DELETE CASCADE,
   html_content text
);

CREATE TABLE video_components (
  component_id uuid PRIMARY KEY REFERENCES components(id) ON DELETE CASCADE,
  vimeo_id text,
  width int,
  height int
);

CREATE TABLE audio_components (
  component_id uuid PRIMARY KEY REFERENCES components(id) ON DELETE CASCADE,
  soundcloud_id text
);

CREATE TABLE component_notes (
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  component_id uuid REFERENCES components(id) ON DELETE RESTRICT,
  document_id uuid REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (user_id, component_id, document_id)
);

/*
  Master records for student work.
*/

CREATE TABLE work (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  task_id uuid NOT NULL REFERENCES tasks(id) ON DELETE RESTRICT,
  version bigint,
  is_complete boolean DEFAULT false,
  work_type int,
  created_at timestamp with time zone,
  updated_at timestamp with time zone,
  UNIQUE (user_id, task_id)
);

/*
  Long Answer answer work is versioned with the document system.
  Question work is versioned in an answers table.
*/

CREATE TABLE document_work (
  work_id uuid REFERENCES work(id) ON DELETE CASCADE,
  document_id uuid REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (work_id, document_id)
);

CREATE TABLE question_work (
  work_id uuid REFERENCES work(id) ON DELETE CASCADE,
  answers jsonb,
  PRIMARY KEY (work_id)
);

CREATE TABLE question_work_answers (
  work_id uuid REFERENCES question_work(work_id) ON DELETE CASCADE,
  version bigint,
  answers jsonb,
  created_at timestamp with time zone,
  PRIMARY KEY (work_id, version)
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


CREATE TABLE journal (
  id uuid PRIMARY KEY,
  version bigint,
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  project_id uuid REFERENCES projects(id) ON DELETE CASCADE,
  entry_type text,
  item text,
  created_at timestamp with time zone,
  updated_at timestamp with time zone
);

CREATE TABLE chat_logs (
  course_id uuid REFERENCES courses(id) ON DELETE CASCADE,
  message_num bigint,
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  message text,
  hidden boolean,
  created_at timestamp with time zone,
  PRIMARY KEY (course_id, message_num)
);

/* YEAR 2014 */
CREATE TABLE journal_201401 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-01-01' AND '2014-01-31T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201402 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-02-01' AND '2014-02-28T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201403 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-03-01' AND '2014-03-31T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201404 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-04-01' AND '2014-04-30T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201405 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-05-01' AND '2014-05-31T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201406 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-06-01' AND '2014-06-30T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201407 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-07-01' AND '2014-07-31T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201408 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-08-01' AND '2014-08-31T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201409 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-09-01' AND '2014-09-30T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201410 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-10-01' AND '2014-10-31T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201411 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-11-01' AND '2014-11-30T23:59:59')
) INHERITS (journal);

CREATE TABLE journal_201412 (
    PRIMARY KEY(id),
    check (created_at BETWEEN '2014-12-01' AND '2014-12-31T23:59:59')
) INHERITS (journal);

CREATE TABLE project_notes (
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  project_id uuid REFERENCES projects(id) ON DELETE RESTRICT,
  document_id uuid REFERENCES documents(id) ON DELETE RESTRICT,
  PRIMARY KEY (user_id, project_id)
);

create table words(word text, lang text, PRIMARY KEY(word, lang));

create TABLE links (
  course_id uuid REFERENCES courses(id) ON DELETE CASCADE,
  link text PRIMARY KEY,
  created_at timestamp with time zone
);

CREATE TABLE teacher_limit (
  teacher_id uuid REFERENCES users(id) ON DELETE CASCADE,
  type text,
  limited integer,
  PRIMARY KEY (teacher_id, type)
);

CREATE TABLE course_limit (
  course_id uuid REFERENCES courses(id) ON DELETE CASCADE,
  type text,
  limited integer,
  PRIMARY KEY (course_id, type)
);

CREATE OR REPLACE FUNCTION get_slug(_slug text, _table text, _id uuid) RETURNS text AS $$
DECLARE
 updatedSlug text := $1;
 counter integer := 1;
 isSlugExist integer := 0;
BEGIN
    EXECUTE 'SELECT count(*) FROM ' || $2 || ' WHERE slug=''' || $1 || '''' || ' AND id !=''' || $3 || '''' INTO isSlugExist;

	  WHILE isSlugExist > 0 LOOP
		   updatedSlug := $1 || '-' || counter;
		   EXECUTE 'SELECT count(*) FROM ' || $2 || ' WHERE slug=''' || updatedSlug || '''' || ' AND id !=''' || $3 || '''' INTO isSlugExist;
		   counter := counter + 1;
	  END LOOP;

	  RETURN updatedSlug;
END; $$
LANGUAGE PLPGSQL;

-- CREATE EXTENSION pg_trgm;
-- CREATE INDEX trgm_idx ON users USING gist (email gist_trgm_ops);







