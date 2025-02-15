alter table user_tokens drop constraint activation_pkey;
alter table user_tokens add constraint token_key  primary key(user_id, token_type);

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
)
