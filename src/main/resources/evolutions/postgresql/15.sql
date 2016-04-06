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