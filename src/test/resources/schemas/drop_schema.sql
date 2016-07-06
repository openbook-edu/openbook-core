DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS users_roles CASCADE;
DROP TABLE IF EXISTS user_tokens CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS users_courses CASCADE;
DROP TABLE IF EXISTS course_schedules CASCADE;
DROP TABLE IF EXISTS course_schedule_exceptions CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
DROP TABLE IF EXISTS parts CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS document_revisions CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS document_tasks CASCADE;
DROP TABLE IF EXISTS question_tasks CASCADE;
DROP TABLE IF EXISTS media_tasks CASCADE;
DROP TABLE IF EXISTS task_feedbacks CASCADE;
DROP TABLE IF EXISTS task_notes CASCADE;
DROP TABLE IF EXISTS components CASCADE;
DROP TABLE IF EXISTS parts_components CASCADE;
DROP TABLE IF EXISTS text_components CASCADE;
DROP TABLE IF EXISTS video_components CASCADE;
DROP TABLE IF EXISTS audio_components CASCADE;
DROP TABLE IF EXISTS book_components CASCADE;
DROP TABLE IF EXISTS generic_html_components CASCADE;
DROP TABLE IF EXISTS rubric_components CASCADE;
DROP TABLE IF EXISTS component_notes CASCADE;
DROP TABLE IF EXISTS work CASCADE;
DROP TABLE IF EXISTS document_work CASCADE;
DROP TABLE IF EXISTS question_work CASCADE;
DROP TABLE IF EXISTS question_work_answers CASCADE;
DROP TABLE IF EXISTS media_work CASCADE;
DROP TABLE IF EXISTS media_work_data CASCADE;
DROP TABLE IF EXISTS journal CASCADE;
DROP TABLE IF EXISTS chat_logs CASCADE;
DROP TABLE IF EXISTS project_notes CASCADE;
DROP TABLE IF EXISTS words CASCADE;
DROP TABLE IF EXISTS links CASCADE;
DROP TABLE IF EXISTS teacher_limit CASCADE;
DROP TABLE IF EXISTS course_limit CASCADE;
DROP TABLE IF EXISTS tags CASCADE;
DROP TABLE IF EXISTS project_tags CASCADE;
DROP TABLE IF EXISTS tag_categories CASCADE;
DROP FUNCTION IF EXISTS get_slug(text, text, uuid) CASCADE;
-- DROP EXTENSION IF EXISTS pg_trgm;