create TABLE links (
  course_id uuid REFERENCES courses(id) ON DELETE CASCADE,
  link text PRIMARY KEY,
  created_at timestamp with time zone
);
alter table links owner to krispii_dev