/* ---------------------- USERS ---------------------- */

/* user A */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('36c8c0ca50aa4806afa5916a5e33a81f', 1, 'testUserA@example.com', 'testUserA', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestA', 'UserA', '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

/* user B */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('6c0e29bdd05b4b2981156be93e936c59', 2, 'testUserB@example.com', 'testUserB', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestB', 'UserB', '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

/* user C */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('f5f984073a0b4ea5952a575886e90586', 3, 'testUserC@example.com', 'testUserC', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestC', 'UserC', '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

/* user E (student) */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('871b525067124e548ab60784cae0bc64', 4, 'testUserE@example.com', 'testUserE', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestE', 'UserE', '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

/* user F */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('4d01347ec5924e5fb09fdd281b3d9b87', 5, 'testUserF@example.com', 'testUserF', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestF', 'UserF', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* user G */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('c4d948967e1b45fabae74fb3a89a4d63', 6, 'testUserG@example.com', 'testUserG', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestG', 'UserG', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* user H no references in other tables */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('5099a6b48809400d8e380119184d0f93', 7, 'testUserH@example.com', 'testUserH', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestH', 'UserH', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* user for activation tests, no references in other tables */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('8b6dc674-d1ae-11e5-9080-08626681851d', 1, 'rafael@krispii.com', 'rafaelya', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'Rafael', 'Yanez', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('7c62a526-d1b0-11e5-9080-08626681851d', 1, 'yanez@krispii.com', 'yanez', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'Antonio', 'Yanez', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');
/* user X for deletion tests with deleted set to true*/
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at, is_deleted)
VALUES ('a898c83a-5638-4483-9528-8037b3ed661d', 1, 'kmccormick@krispii.com', 'mysterion', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'Kenny', 'McCormick', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04', TRUE);

/* ---------------------- ROLES ---------------------- */

/* role A */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('1430e95077f94b30baf8bb226fc7091a', 1, 'test role A', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* role B */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('a011504cd11840cdb9eb6e10d5738c67', 2, 'test role B', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* role C */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('31a4c2e6762a4303bbb8e64c24048920', 3, 'test role C', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* role F */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('45b3552707ad4c4f9051f0e755216163', 4, 'test role F', '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

/* role G */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('45cc7cc8987645f58efeccd4dba7ea69', 5, 'test role G', '2014-08-17 14:01:19.545-04', '2014-08-18 14:01:19.545-04');

/* role H */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('2a3edf38750a46aa84289fb08e648ee8', 6, 'test role H', '2014-08-19 14:01:19.545-04', '2014-08-20 14:01:19.545-04');


/* ---------------------- USERS_ROLES ---------------------- */

/* UserA -> RoleA */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('36c8c0ca50aa4806afa5916a5e33a81f', '1430e95077f94b30baf8bb226fc7091a', '2014-08-13 14:01:19.545-04');

/* UserA -> RoleB */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('36c8c0ca50aa4806afa5916a5e33a81f', 'a011504cd11840cdb9eb6e10d5738c67', '2014-08-14 14:01:19.545-04');

/* UserA -> RoleF */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('36c8c0ca50aa4806afa5916a5e33a81f', '45b3552707ad4c4f9051f0e755216163', '2014-08-14 14:01:19.545-04');

/* UserA -> RoleG */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('36c8c0ca50aa4806afa5916a5e33a81f', '45cc7cc8987645f58efeccd4dba7ea69', '2014-08-14 14:01:19.545-04');

/* UserB -> RoleA */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('6c0e29bdd05b4b2981156be93e936c59', '1430e95077f94b30baf8bb226fc7091a', '2014-08-15 14:01:19.545-04');

/* UserB -> RoleB */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('6c0e29bdd05b4b2981156be93e936c59', 'a011504cd11840cdb9eb6e10d5738c67', '2014-08-16 14:01:19.545-04');

/* UserB -> RoleF */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('6c0e29bdd05b4b2981156be93e936c59', '45b3552707ad4c4f9051f0e755216163', '2014-08-17 14:01:19.545-04');

/* UserF -> RoleC */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('4d01347ec5924e5fb09fdd281b3d9b87', '31a4c2e6762a4303bbb8e64c24048920', '2014-08-19 14:01:19.545-04');


/* ---------------------- COURSES ---------------------- */

/* course A -> user A (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, enabled, chat_enabled, scheduling_enabled, created_at, updated_at)
VALUES ('217c5622ff9e43728e6a95fb3bae300b', 1, '36c8c0ca50aa4806afa5916a5e33a81f', 'test course A', 1574408, 'test-course-A-slug', true, true, false, '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* course B -> user B (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, enabled, chat_enabled, scheduling_enabled, created_at, updated_at)
VALUES ('404c800a53854e6b867e365a1e6b00de', 2, '6c0e29bdd05b4b2981156be93e936c59', 'test course B', 2230288, 'test-course-B-slug', false, true, false, '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* course D -> user F (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, enabled, chat_enabled, scheduling_enabled, created_at, updated_at)
VALUES ('94cc65bb45424f628e08d58522e7b5f1', 3, '4d01347ec5924e5fb09fdd281b3d9b87', 'test course D', 269368, 'test-course-D-slug', false, true, false, '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* course F -> user F (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, enabled, chat_enabled, scheduling_enabled, created_at, updated_at)
VALUES ('287b61f5da6b4de785353bc500cffac7', 4, '4d01347ec5924e5fb09fdd281b3d9b87', 'test course F', 269368, 'test-course-F-slug', false, true, false, '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

/* course G -> user F (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, enabled, chat_enabled, scheduling_enabled, created_at, updated_at)
VALUES ('b24abba8e6c74700900ce66ed0185a70', 5, '4d01347ec5924e5fb09fdd281b3d9b87', 'test course G', 1508909, 'test-course-G-slug', false, true, false, '2014-08-17 14:01:19.545-04', '2014-08-18 14:01:19.545-04');

/* course K   */
INSERT INTO courses (id, version, teacher_id, name, color, slug, enabled, chat_enabled, scheduling_enabled, created_at, updated_at)
VALUES ('b24abba8e6c74700900ce66ed0185a71', 5, '4d01347ec5924e5fb09fdd281b3d9b87', 'test course K', 1508909, 'test-course-K-slug', false, true, false, '2014-08-17 14:01:19.545-04', '2014-08-18 14:01:19.545-04');

/* ---------------------- USERS_COURSES ---------------------- */

/* UserC (student) -> CourseA -> user A (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('f5f984073a0b4ea5952a575886e90586', '217c5622ff9e43728e6a95fb3bae300b', '2014-08-05 14:01:19.545-04');

/* UserC (student) -> CourseB -> user B (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('f5f984073a0b4ea5952a575886e90586', '404c800a53854e6b867e365a1e6b00de', '2014-08-06 14:01:19.545-04');

/* UserE (student) -> CourseB -> user B (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('871b525067124e548ab60784cae0bc64', '404c800a53854e6b867e365a1e6b00de', '2014-08-07 14:01:19.545-04');

/* UserE (student) -> CourseA -> user A (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('871b525067124e548ab60784cae0bc64', '217c5622ff9e43728e6a95fb3bae300b', '2014-08-11 14:01:19.545-04');

/* UserG (student) -> CourseF -> user F (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('c4d948967e1b45fabae74fb3a89a4d63', '287b61f5da6b4de785353bc500cffac7', '2014-08-08 14:01:19.545-04');

/* UserH (student) -> CourseF -> user F (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('5099a6b48809400d8e380119184d0f93', '287b61f5da6b4de785353bc500cffac7', '2014-08-10 14:01:19.545-04');

/* UserH (student) -> CourseD -> user F (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('5099a6b48809400d8e380119184d0f93', '94cc65bb45424f628e08d58522e7b5f1', '2014-08-10 14:01:19.545-04');


/* ---------------------- PROJECTS ---------------------- */

/* project A -> course A -> user A (teacher) */
INSERT INTO projects (id, course_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('c9b4cfceaed448fd94f5c980763dfddc', '217c5622ff9e43728e6a95fb3bae300b', 1, 'test project A', 'test-project-slug-A', 'test project A description', 'any', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* project B -> course B -> user B (teacher) */
INSERT INTO projects (id, course_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('e4ae3b9098714339b05c8d39e3aaf65d', '404c800a53854e6b867e365a1e6b00de', 2, 'test project B', 'test-project-slug-B', 'test project B description', 'free', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* project C -> course B -> user B (teacher) */
INSERT INTO projects (id, course_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('4ac4d872451b4092b13f643d6d5fa930', '404c800a53854e6b867e365a1e6b00de', 3, 'test project C', 'test-project-slug-C', 'test project C description', 'course', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* project E -> course A -> user A (teacher) */
INSERT INTO projects (id, course_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('b36919cb2df043b7bb7f36cae797deaa', '217c5622ff9e43728e6a95fb3bae300b', 4, 'test project E', 'test-project-slug-E', 'test project E description', 'course', '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

/*  project F master project */
INSERT INTO projects (id, course_id, version, name, slug, description, availability,  is_master, enabled, created_at, updated_at)
VALUES ('b36919cb2df043b7bb7f36cae797deab', 'b24abba8e6c74700900ce66ed0185a70', 1, 'test project F', 'test-project-slug-F', 'test project F description', 'course', true, true, '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

/* project G master project */
INSERT INTO projects (id, course_id, version, name, slug, description, availability,  is_master, enabled, created_at, updated_at)
VALUES ('b36919cb2df043b7bb7f36cae797deac', 'b24abba8e6c74700900ce66ed0185a70', 1, 'test project G', 'test-project-slug-G', 'test project G description', 'course', true, true, '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');
/* project H master project */
INSERT INTO projects (id, course_id, version, name, slug, description, availability,  is_master, enabled, created_at, updated_at)
VALUES ('00743ada1d3a4912adc8fb8a0b1b7447', 'b24abba8e6c74700900ce66ed0185a70', 1, 'test project H', 'test-project-slug-H', 'test project H description', 'course', true, true, '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');
/* ---------------------- PARTS ---------------------- */

/* part A -> project A -> course A -> user A (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('5cd214be6bba47fa9f350eb8bafec397', 1, 'c9b4cfceaed448fd94f5c980763dfddc', 'test part A', true, 10, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

/* part B -> project A -> course A -> user A (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('abb84847a3d247a0ae7d8ce04063afc7', 2, 'c9b4cfceaed448fd94f5c980763dfddc', 'test part B', false, 11, '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

/* part C -> project B -> course B -> user B (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('fb01f11b7f2341c8877b68410be62aa5', 3, 'e4ae3b9098714339b05c8d39e3aaf65d', 'test part C', true, 12, '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

/* part E -> project C -> course B -> user B (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('c850ec53f0a9460d918a5e6fd538f376', 4, '4ac4d872451b4092b13f643d6d5fa930', 'test part E', false, 13, '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

/* part F -> project C -> course B -> user B (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('e8d526846afd48e58049a179e8868432', 5, '4ac4d872451b4092b13f643d6d5fa930', 'test part F', true, 14, '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* part G -> project A -> course A -> user A (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('8e080c002b204e7bb18c2582d79e7e68', 6, 'c9b4cfceaed448fd94f5c980763dfddc', 'test part G', true, 15, '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* part H -> project C -> course B -> user B (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('45a146b3fd9a4cab9d1d3e9b0b15e12c', 7, '4ac4d872451b4092b13f643d6d5fa930', 'test part H', true, 16, '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');


/* ---------------------- TASKS ---------------------- */

/* longAnswerTask A -> part A -> project A -> course A -> user A (teacher)*/
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('bf1a6ed09f834cb485c1ad456299b3a3', 1, '5cd214be6bba47fa9f350eb8bafec397', 'test longAnswerTask A', 'test longAnswerTask A description', 10, 0, true, 'test longAnswerTask A response title', 'test longAnswerTask A notes title', 'test help text testLongAnswerTaskA', '50', '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

INSERT INTO document_tasks (task_id, dependency_id)
VALUES ('bf1a6ed09f834cb485c1ad456299b3a3', null);

/* longAnswerTask N -> part A -> project A -> course A -> user A (teacher)*/
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('73b75cd0c796429fa73402fabca367aa', 2, '5cd214be6bba47fa9f350eb8bafec397', 'test longAnswerTask N', 'test longAnswerTask N description', 26, 0, true, 'test longAnswerTask N response title', 'test longAnswerTask N notes title', 'test help text testLongAnswerTaskN', '25', '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

INSERT INTO document_tasks (task_id, dependency_id)
VALUES ('73b75cd0c796429fa73402fabca367aa', null);

/* longAnswerTask O -> part A -> project A -> course A -> user A (teacher)*/
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('b807dbf4b4fe4b7bb1b711083899470d', 3, '5cd214be6bba47fa9f350eb8bafec397', 'test longAnswerTask O', 'test longAnswerTask O description', 27, 0, true, 'test longAnswerTask O response title', 'test longAnswerTask O notes title', 'test help text testLongAnswerTaskO', '15', '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

INSERT INTO document_tasks (task_id, dependency_id)
VALUES ('b807dbf4b4fe4b7bb1b711083899470d', '73b75cd0c796429fa73402fabca367aa');


/* shortAnswerTask B -> part A -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('10ef05ee7b494352b86e70510adf617f', 2, '5cd214be6bba47fa9f350eb8bafec397', 'test shortAnswerTask B', 'test shortAnswerTask B description', 14, 1, true, 'test shortAnswerTask B response title', 'test shortAnswerTask B notes title', 'test help text testShortAnswerTaskB', '90', '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

INSERT INTO question_tasks (task_id, questions)
VALUES ('10ef05ee7b494352b86e70510adf617f', '[{"id": "9d2ed6e8-ccdd-474f-9583-4d10eafaa2a6", "type": 1, "title": "testShortQuestionA title", "description": "testShortQuestionA description", "maxLength": 51}]');


/* multipleChoiceTask C -> part A -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('76cc2ed7611b4dafaa3f20efe42a65a0', 3, '5cd214be6bba47fa9f350eb8bafec397', 'test MultipleChoiceTask C', 'test MultipleChoiceTask C description', 16, 1, true, 'test MultipleChoiceTask C response title', 'test MultipleChoiceTask C notes title', 'test help text testMultipleChoiceTaskC', '30', '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

INSERT INTO question_tasks (task_id, questions)
VALUES ('76cc2ed7611b4dafaa3f20efe42a65a0', '[{"id": "d7a0c042-4d4b-4f31-bc19-59a2b6659e3c", "type": 3, "title": "testMultipleChoiceQuestionB title", "description": "testMultipleChoiceQuestionB description", "choices": ["choice 1", "choice 2"], "correct": [1, 2], "singleAnswer": true}]');


/* orderingTask D -> part B -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('808400838923476fa8738ba6c55e30c8', 4, 'abb84847a3d247a0ae7d8ce04063afc7', 'test OrderingTask D', 'test OrderingTask D description', 18, 1, true, 'test OrderingTask D response title', 'test OrderingTask D notes title', 'test help text testOrderingTaskD', '1', '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

INSERT INTO question_tasks (task_id, questions)
VALUES ('808400838923476fa8738ba6c55e30c8', '[{"id": "667a8b1c-3230-43d4-bd7b-2b150205b109", "type": 4, "title": "testOrderingQuestionC title", "description": "testOrderingQuestionC description", "choices": ["choice 3", "choice 4"]}]');


/* orderingTask L -> part B  -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('3d3578bd60d34aeabe070359dad2fecb', 6, 'abb84847a3d247a0ae7d8ce04063afc7', 'test OrderingTask L', 'test OrderingTask L description', 19, 1, true, 'test OrderingTask L response title', 'test OrderingTask L notes title', 'test help text testOrderingTaskL', '77', '2014-08-08 14:01:19.545-04', '2014-08-09 14:01:19.545-04');

INSERT INTO question_tasks (task_id, questions)
VALUES ('3d3578bd60d34aeabe070359dad2fecb','[{"id": "667a8b1c-3230-43d4-bd7b-2b150205b109", "type": 4, "title": "testOrderingQuestionC title", "description": "testOrderingQuestionC description", "choices": ["choice 3", "choice 4"]}]');


/* orderingTask N -> part G -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('599a78ad5bff4246983532fcb41168a6', 7, '8e080c002b204e7bb18c2582d79e7e68', 'test OrderingTask N', 'test OrderingTask N description', 20, 1, true, 'test OrderingTask N response title', 'test OrderingTask N notes title', 'test help text testOrderingTaskN', '1000', '2014-08-10 14:01:19.545-04', '2014-08-11 14:01:19.545-04');

INSERT INTO question_tasks (task_id, questions)
VALUES ('599a78ad5bff4246983532fcb41168a6', '[{"id": "667a8b1c-3230-43d4-bd7b-2b150205b109", "type": 4, "title": "testOrderingQuestionC title", "description": "testOrderingQuestionC description", "choices": ["choice 3", "choice 4"]}]');


/* matchingTask E -> part C -> project B -> course B -> user B (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('468a35bfbaf84045aa184688f4d0721f', 5, 'fb01f11b7f2341c8877b68410be62aa5', 'test MatchingTask E', 'test MatchingTask E description', 1, 1, true, 'test MatchingTask E response title', 'test MatchingTask E notes title', 'test help text testMatchingTaskE', '5', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

INSERT INTO question_tasks (task_id, questions)
VALUES ('468a35bfbaf84045aa184688f4d0721f', '[{"id": "1646f580-2347-4cdd-8b7d-fd43588a3a50", "type": 5, "title": "testMatchingQuestionD title", "description": "testMatchingQuestionD description", "choices": [{"left": "choice left 5", "right": "choice right 6"}, {"left": "choice left 7", "right": "choice right 8"}]}]');


/* matchingTask K -> partB -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, max_grade, created_at, updated_at)
VALUES ('337fa73136854ba38668280c0096514c', 7, 'abb84847a3d247a0ae7d8ce04063afc7', 'test MatchingTask K', 'test MatchingTask K description', 24, 1, true, 'test MatchingTask K response title', 'test MatchingTask K notes title', 'test help text testMatchingTaskK', '100', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

INSERT INTO question_tasks (task_id, questions)
VALUES ('337fa73136854ba38668280c0096514c', '[{"id": "1646f580-2347-4cdd-8b7d-fd43588a3a50", "type": 5, "title": "testMatchingQuestionD title", "description": "testMatchingQuestionD description", "choices": [{"left": "choice left 5", "right": "choice right 6"}, {"left": "choice left 7", "right": "choice right 8"}]}]');


/* matchingTask M -> partB -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, created_at, updated_at)
VALUES ('129f2b0856d34e14aa5b659f53f71e39', 8, 'abb84847a3d247a0ae7d8ce04063afc7', 'test MatchingTask M', 'test MatchingTask M description', 25, 1, true, 'test MatchingTask M response title', 'test MatchingTask M notes title', 'test help text testMatchingTaskM', '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

INSERT INTO question_tasks (task_id, questions)
VALUES ('129f2b0856d34e14aa5b659f53f71e39', '[
{"id": "1646f580-2347-4cdd-8b7d-fd43588a3a50", "type": 5, "title": "testMatchingQuestionD title", "description": "testMatchingQuestionD description", "choices": [{"left": "choice left 5", "right": "choice right 6"}, {"left": "choice left 7", "right": "choice right 8"}]}
]');


/* blanksTask P -> part G -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, created_at, updated_at)
VALUES ('9a258f444ee84bab855b53966e53ca10', 9, '8e080c002b204e7bb18c2582d79e7e68', 'test BlanksTask P', 'test BlanksTask P description', 28, 1, true, 'test BlanksTask P response title', 'test BlanksTask P notes title', 'test help text testBlanksTaskP', '2014-08-17 14:01:19.545-04', '2014-08-18 14:01:19.545-04');

INSERT INTO question_tasks (task_id, questions)
VALUES ('9a258f444ee84bab855b53966e53ca10', '[
{"id": "67a68639-0172-4948-88f0-57e77a7502d7", "type": 2, "title": "testBlanksQuestionE title", "description": "testBlanksQuestionE description", "text": "testBlanksQuestionE text", "inputs": [{"position": 5, "maxLength": 50}, {"position": 6, "maxLength": 60}] },
{"id": "1646f580-2347-4cdd-8b7d-fd43588a3a50", "type": 5, "title": "testMatchingQuestionD title", "description": "testMatchingQuestionD description", "choices": [{"left": "choice left 5", "right": "choice right 6"}, {"left": "choice left 7", "right": "choice right 8"}]}
]');

/* mediaTask A -> part A -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, name, description, position, task_type, notes_allowed, response_title, notes_title, help_text, created_at, updated_at)
VALUES ('a7121b74eac111e59ce95e5517507c66', 3, '5cd214be6bba47fa9f350eb8bafec397', 'test MediaTask A', 'test MediaTask A description', 16, 2, true, 'test MediaTask A response title', 'test MediaTask A notes title', 'test help text testMediaTaskA', '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

INSERT INTO media_tasks (task_id, media_type)
VALUES ('a7121b74eac111e59ce95e5517507c66', 0);

/* ---------------------- DOCUMENTS ---------------------- */

/* documentA -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('fd923b3f6dc2472e8ce77a8fcc6a1a20', 2, 'f5f984073a0b4ea5952a575886e90586', 'testDocumentA title', '{"ops":[{"insert":"Hello Sam"}]}', '2014-08-01 14:01:19.545-04', '2014-08-03 14:01:19.545-04');

/* documentB -> userE */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('15173757b881444082854e3d2c03616a', 2, '871b525067124e548ab60784cae0bc64', 'testDocumentB title', '{"ops":[{"insert":"Hello Dean"}]}', '2014-08-03 14:01:19.545-04', '2014-08-05 14:01:19.545-04');

/* documentC -> userE */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('462b7f6c8b624c998643a63b2720b2a7', 2, '871b525067124e548ab60784cae0bc64', 'testDocumentC title', '{"ops":[{"insert":"Hello Jhonatan"}]}', '2014-08-03 14:01:19.545-04', '2014-08-05 14:01:19.545-04');

/* documentD -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('bd01c98803694ddaada205a9ff3645cf', 2, 'f5f984073a0b4ea5952a575886e90586', 'testDocumentD title', '{"ops":[{"insert":"Hello Morgan"}]}', '2014-08-05 14:01:19.545-04', '2014-08-07 14:01:19.545-04');

/* documentF -> userA (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('1a9d5407b3c444a18e7e1d7e9578eabc', 2, '36c8c0ca50aa4806afa5916a5e33a81f', 'testDocumentF title', '{"ops":[{"insert":"Hello Jason"}]}', '2014-08-09 14:01:19.545-04', '2014-08-11 14:01:19.545-04');

/* documentG -> userA (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('300ddfb7f9bf47fea0b226f332828fff', 2, '36c8c0ca50aa4806afa5916a5e33a81f', 'testDocumentG title', '{"ops":[{"insert":"Hello Moony"}]}', '2014-08-11 14:01:19.545-04', '2014-08-13 14:01:19.545-04');

/* documentH -> userB (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('eb8ef353d22f48a4a356351e0de3ed16', 2, '6c0e29bdd05b4b2981156be93e936c59', 'testDocumentH title', '{"ops":[{"insert":"Hello Flipper"}]}', '2014-08-13 14:01:19.545-04', '2014-08-15 14:01:19.545-04');

/* documentI -> userB (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('9110c16f45fd42119e39b15ab8b6f9ee', 2, '6c0e29bdd05b4b2981156be93e936c59', 'testDocumentI title', '{"ops":[{"insert":"Hello Groovy"}]}', '2014-08-15 14:01:19.545-04', '2014-08-17 14:01:19.545-04');

/* documentJ -> userA (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('30739c6d43774a2f8aa3d1240dfb0740', 2, '36c8c0ca50aa4806afa5916a5e33a81f', 'testDocumentJ title', '{"ops":[{"insert":"Hello Bobby"}]}', '2014-08-17 14:01:19.545-04', '2014-08-19 14:01:19.545-04');

/* documentK -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('2f1180f017f4488b9f03ad8fbfbeaf3a', 2, 'f5f984073a0b4ea5952a575886e90586', 'testDocumentK title', '{"ops":[{"insert":"Hello Moris"}]}', '2014-08-19 14:01:19.545-04', '2014-08-21 14:01:19.545-04');

/* documentL -> userE */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('7c9d0daefe794eccb36cc141a4122fab', 2, '871b525067124e548ab60784cae0bc64', 'testDocumentL title', '{"ops":[{"insert":"Hello Boris"}]}', '2014-08-21 14:01:19.545-04', '2014-08-23 14:01:19.545-04');

/* documentM -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('0ed856aafd4c486db6c5293ca18c37dd', 2, 'f5f984073a0b4ea5952a575886e90586', 'testDocumentM title', '{"ops":[{"insert":"Hello Vasea"}]}', '2014-08-23 14:01:19.545-04', '2014-08-25 14:01:19.545-04');

/* documentN -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('196a1793c6884f66b725a8353dd1ac67', 2, 'f5f984073a0b4ea5952a575886e90586', 'testDocumentN title', '{"ops":[{"insert":"Hello Petea"}]}', '2014-08-25 14:01:19.545-04', '2014-08-27 14:01:19.545-04');

/* documentO -> userE */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('78b9baaf16b743a39cec410104cdde4e', 2, '871b525067124e548ab60784cae0bc64', 'testDocumentO title', '{"ops":[{"insert":"Hello Doris"}]}', '2014-08-27 14:01:19.545-04', '2014-08-29 14:01:19.545-04');


/* ---------------------- DOCUMENT_REVISIONS ---------------------- */

/* CurrentRevisionA */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('fd923b3f6dc2472e8ce77a8fcc6a1a20', 2, 'f5f984073a0b4ea5952a575886e90586', '{"ops":[{"delete":7},{"insert":"Hello"},{"retain":4}]}', '2014-08-03 14:01:19.545-04');

/* PreviousRevisionA */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('fd923b3f6dc2472e8ce77a8fcc6a1a20', 1, 'f5f984073a0b4ea5952a575886e90586', '{"ops":[{"insert":"Goodbye Sam"}]}', '2014-08-02 14:01:19.545-04');

/* CurrentRevisionB */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('15173757b881444082854e3d2c03616a', 2, '871b525067124e548ab60784cae0bc64', '{"ops":[{"delete":7},{"insert":"Hello"},{"retain":5}]}', '2014-08-05 14:01:19.545-04');

/* PreviousRevisionB */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('15173757b881444082854e3d2c03616a', 1, '871b525067124e548ab60784cae0bc64', '{"ops":[{"insert":"Goodbye Dean"}]}', '2014-08-04 14:01:19.545-04');

/* CurrentRevisionC */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('462b7f6c8b624c998643a63b2720b2a7', 2, '871b525067124e548ab60784cae0bc64', '{"ops":[{"delete":7},{"insert":"Hello"},{"retain":9}]}', '2014-08-05 14:01:19.545-04');

/* PreviousRevisionC */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('462b7f6c8b624c998643a63b2720b2a7', 1, '871b525067124e548ab60784cae0bc64', '{"ops":[{"insert":"Goodbye Jhonatan"}]}', '2014-08-04 14:01:19.545-04');

/* CurrentRevisionD */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('bd01c98803694ddaada205a9ff3645cf', 2, 'f5f984073a0b4ea5952a575886e90586', '{"ops":[{"delete":7},{"insert":"Hello"},{"retain":7}]}', '2014-08-07 14:01:19.545-04');

/* PreviousRevisionD */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('bd01c98803694ddaada205a9ff3645cf', 1, 'f5f984073a0b4ea5952a575886e90586', '{"ops":[{"insert":"Goodbye Morgan"}]}', '2014-08-06 14:01:19.545-04');


/* ---------------------- TASK_FEEDBACKS ---------------------- */

/* taskFeedbackA -> longAnswerTaskA */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('f5f984073a0b4ea5952a575886e90586', 'bf1a6ed09f834cb485c1ad456299b3a3', '1a9d5407b3c444a18e7e1d7e9578eabc');

/* taskFeedbackB -> shortAnswerTaskB */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('871b525067124e548ab60784cae0bc64', '10ef05ee7b494352b86e70510adf617f', '300ddfb7f9bf47fea0b226f332828fff');

/* taskFeedbackC -> matchingTaskE */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('f5f984073a0b4ea5952a575886e90586', '468a35bfbaf84045aa184688f4d0721f', 'eb8ef353d22f48a4a356351e0de3ed16');

/* taskFeedbackD -> matchingTaskE */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('871b525067124e548ab60784cae0bc64', '468a35bfbaf84045aa184688f4d0721f', '9110c16f45fd42119e39b15ab8b6f9ee');

/* taskFeedbackE -> orderingTaskN */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('f5f984073a0b4ea5952a575886e90586', '599a78ad5bff4246983532fcb41168a6', '30739c6d43774a2f8aa3d1240dfb0740');


/* ---------------------- TASK_SCRATCHPADS ---------------------- */

/* taskScratchpadA */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('f5f984073a0b4ea5952a575886e90586', 'bf1a6ed09f834cb485c1ad456299b3a3', '2f1180f017f4488b9f03ad8fbfbeaf3a');

/* taskScratchpadB */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('871b525067124e548ab60784cae0bc64', '10ef05ee7b494352b86e70510adf617f', '7c9d0daefe794eccb36cc141a4122fab');

/* taskScratchpadC */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('f5f984073a0b4ea5952a575886e90586', '468a35bfbaf84045aa184688f4d0721f', '196a1793c6884f66b725a8353dd1ac67');

/* taskScratchpadD */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('871b525067124e548ab60784cae0bc64', '468a35bfbaf84045aa184688f4d0721f', '78b9baaf16b743a39cec410104cdde4e');

/* taskScratchpadE */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('f5f984073a0b4ea5952a575886e90586', '599a78ad5bff4246983532fcb41168a6', '0ed856aafd4c486db6c5293ca18c37dd');


/* ----------------------- WORK ---------------------- */

/* longAnswerWorkA -> userC -> longAnswerTaskA -> documentA */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('441374e20b1643ecadb96a3251081d24', 'f5f984073a0b4ea5952a575886e90586', 'bf1a6ed09f834cb485c1ad456299b3a3', 1, true, 0, '2014-08-01 14:01:19.545-04', '2014-08-03 14:01:19.545-04');

INSERT INTO document_work (work_id, document_id)
VALUES ('441374e20b1643ecadb96a3251081d24', 'fd923b3f6dc2472e8ce77a8fcc6a1a20');


/* longAnswerWorkF -> userE -> longAnswerTaskA -> documentB */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('f7fcffc37b794de7b6ddcf37aa155fd9', '871b525067124e548ab60784cae0bc64', 'bf1a6ed09f834cb485c1ad456299b3a3', 1, true, 0, '2014-08-03 14:01:19.545-04', '2014-08-05 14:01:19.545-04');

INSERT INTO document_work (work_id, document_id)
VALUES ('f7fcffc37b794de7b6ddcf37aa155fd9', '15173757b881444082854e3d2c03616a');


/* ShortAnswerWorkB -> userE -> shortAnswerTask B -> documentC */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('cbf452cd915a4b249d0292be013bbba8', '871b525067124e548ab60784cae0bc64', '10ef05ee7b494352b86e70510adf617f', 2, false, 1, '2014-08-03 14:01:19.545-04', '2014-08-05 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('cbf452cd915a4b249d0292be013bbba8', '{ "9d2ed6e8-ccdd-474f-9583-4d10eafaa2a6": {"questionType": 1, "answer": "testShortAnswerA answer"} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('cbf452cd915a4b249d0292be013bbba8', '{ "9d2ed6e8-ccdd-474f-9583-4d10eafaa2a6": {"questionType": 1, "answer": "testShortAnswerA answer"} }', 2, '2014-08-05 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('cbf452cd915a4b249d0292be013bbba8', '{ "9d2ed6e8-ccdd-474f-9583-4d10eafaa2a6": {"questionType": 1, "answer": "testShortAnswerRevisionA answer"} }', 1, '2014-08-03 14:01:19.545-04');


/* ShortAnswerWorkG -> userC -> shortAnswerTask B -> documentD */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('b7bb09c16aca40de81525da483a5c476', 'f5f984073a0b4ea5952a575886e90586', '10ef05ee7b494352b86e70510adf617f', 2, false, 1, '2014-08-05 14:01:19.545-04', '2014-08-07 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('b7bb09c16aca40de81525da483a5c476', '{ "9d2ed6e8-ccdd-474f-9583-4d10eafaa2a6": {"questionType": 1, "answer": "testShortAnswerA answer"} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('b7bb09c16aca40de81525da483a5c476', '{ "9d2ed6e8-ccdd-474f-9583-4d10eafaa2a6": {"questionType": 1, "answer": "testShortAnswerA answer"} }', 2 ,'2014-08-07 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('b7bb09c16aca40de81525da483a5c476', '{ "9d2ed6e8-ccdd-474f-9583-4d10eafaa2a6": {"questionType": 1, "answer": "testShortAnswerRevisionA answer"} }', 1 ,'2014-08-05 14:01:19.545-04');


/* MultipleChoiceWorkC -> userC -> multipleChoiceTask C */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('edfd619897b04f219e15fbe4ed051970', 'f5f984073a0b4ea5952a575886e90586', '76cc2ed7611b4dafaa3f20efe42a65a0', 3, true, 1, '2014-08-05 14:01:19.545-04', '2014-08-07 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('edfd619897b04f219e15fbe4ed051970', '{"d7a0c042-4d4b-4f31-bc19-59a2b6659e3c": {"questionType": 3, "answer": [1, 2]} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('edfd619897b04f219e15fbe4ed051970', '{"d7a0c042-4d4b-4f31-bc19-59a2b6659e3c": {"questionType": 3, "answer": [1, 2]} }', 3, '2014-08-07 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('edfd619897b04f219e15fbe4ed051970', '{"d7a0c042-4d4b-4f31-bc19-59a2b6659e3c": {"questionType": 3, "answer": [3, 4]} }', 2, '2014-08-05 14:01:19.545-04');


/* MultipleChoiceWorkH -> userE -> multipleChoiceTask C */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('8f3b9f09db434670b1590763eb4eaecd', '871b525067124e548ab60784cae0bc64', '76cc2ed7611b4dafaa3f20efe42a65a0', 8, true, 1, '2014-08-07 14:01:19.545-04', '2014-08-09 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('8f3b9f09db434670b1590763eb4eaecd', '{ "d7a0c042-4d4b-4f31-bc19-59a2b6659e3c": {"questionType": 3, "answer": [1, 2]} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('8f3b9f09db434670b1590763eb4eaecd', '{ "d7a0c042-4d4b-4f31-bc19-59a2b6659e3c": {"questionType": 3, "answer": [1, 2]} }', 8, '2014-08-09 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('8f3b9f09db434670b1590763eb4eaecd', '{ "d7a0c042-4d4b-4f31-bc19-59a2b6659e3c": {"questionType": 3, "answer": [3, 4]} }', 7, '2014-08-08 14:01:19.545-04');


/* OrderingWorkD -> userC -> orderingTaskN */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('125eef5a7e89441cb138c1803bafdc03', 'f5f984073a0b4ea5952a575886e90586', '599a78ad5bff4246983532fcb41168a6', 4, true, 1, '2014-08-07 14:01:19.545-04', '2014-08-09 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('125eef5a7e89441cb138c1803bafdc03', '{ "667a8b1c-3230-43d4-bd7b-2b150205b109": {"questionType": 4, "answer": [2, 3]} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('125eef5a7e89441cb138c1803bafdc03', '{ "667a8b1c-3230-43d4-bd7b-2b150205b109": {"questionType": 4, "answer": [2, 3]} }', 5, '2014-08-09 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('125eef5a7e89441cb138c1803bafdc03', '{ "667a8b1c-3230-43d4-bd7b-2b150205b109": {"questionType": 4, "answer": [5, 6]} }', 3, '2014-08-08 14:01:19.545-04');


/* OrderingWorkI -> userE -> orderingTaskN */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('db5165f44d4840079191beecd77763c7', '871b525067124e548ab60784cae0bc64', '599a78ad5bff4246983532fcb41168a6', 5, true, 1, '2014-08-09 14:01:19.545-04', '2014-08-11 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('db5165f44d4840079191beecd77763c7', '{ "667a8b1c-3230-43d4-bd7b-2b150205b109": {"questionType": 4, "answer": [2, 3]} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('db5165f44d4840079191beecd77763c7', '{ "667a8b1c-3230-43d4-bd7b-2b150205b109": {"questionType": 4, "answer": [2, 3]} }', 5, '2014-08-11 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('db5165f44d4840079191beecd77763c7', '{ "667a8b1c-3230-43d4-bd7b-2b150205b109": {"questionType": 4, "answer": [5, 6]} }', 4, '2014-08-10 14:01:19.545-04');


/* testMatchingWorkE -> userC -> testMatchingTaskE */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('e47442dd8ac94d06ad6fef62720d4ed3', 'f5f984073a0b4ea5952a575886e90586', '468a35bfbaf84045aa184688f4d0721f', 5, true, 1, '2014-08-09 14:01:19.545-04', '2014-08-11 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('e47442dd8ac94d06ad6fef62720d4ed3', '{ "1646f580-2347-4cdd-8b7d-fd43588a3a50": {"questionType": 5, "answer": [ {"left": 2, "right": 3}, {"left": 4, "right": 5} ]} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('e47442dd8ac94d06ad6fef62720d4ed3', '{ "1646f580-2347-4cdd-8b7d-fd43588a3a50": {"questionType": 5, "answer": [ {"left": 2, "right": 3}, {"left": 4, "right": 5} ]} }', 5, '2014-08-11 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('e47442dd8ac94d06ad6fef62720d4ed3', '{ "1646f580-2347-4cdd-8b7d-fd43588a3a50": {"questionType": 5, "answer": [ {"left": 6, "right": 7}, {"left": 8, "right": 9} ]} }', 4, '2014-08-10 14:01:19.545-04');


/* testMatchingWorkJ -> userE -> testMatchingTaskE */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('c57e033551da41449dfcdfa97f5f1a7c', '871b525067124e548ab60784cae0bc64', '468a35bfbaf84045aa184688f4d0721f', 6, false, 1, '2014-08-10 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('c57e033551da41449dfcdfa97f5f1a7c', '{ "1646f580-2347-4cdd-8b7d-fd43588a3a50": {"questionType": 5, "answer": [ {"left": 2, "right": 3}, {"left": 4, "right": 5} ]} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('c57e033551da41449dfcdfa97f5f1a7c', '{ "1646f580-2347-4cdd-8b7d-fd43588a3a50": {"questionType": 5, "answer": [ {"left": 2, "right": 3}, {"left": 4, "right": 5} ]} }', 6, '2014-08-12 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('c57e033551da41449dfcdfa97f5f1a7c', '{ "1646f580-2347-4cdd-8b7d-fd43588a3a50": {"questionType": 5, "answer": [ {"left": 6, "right": 7}, {"left": 8, "right": 9} ]} }', 5, '2014-08-11 14:01:19.545-04');


/* testBlanksWorkK -> userC -> testBlanksTaskP */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('507cd2f2486948f1ab19812c6a1ef5ed', 'f5f984073a0b4ea5952a575886e90586', '9a258f444ee84bab855b53966e53ca10', 4, true, 1, '2014-08-09 14:01:19.545-04', '2014-08-11 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('507cd2f2486948f1ab19812c6a1ef5ed', '{ "67a68639-0172-4948-88f0-57e77a7502d7": {"questionType": 2, "answer": [ "testBlanksAnswerE answer one", "testBlanksAnswerE answer two" ]} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('507cd2f2486948f1ab19812c6a1ef5ed', '{ "67a68639-0172-4948-88f0-57e77a7502d7": {"questionType": 2, "answer": [ "testBlanksAnswerE answer one", "testBlanksAnswerE answer two" ]} }', 4, '2014-08-11 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('507cd2f2486948f1ab19812c6a1ef5ed', '{ "67a68639-0172-4948-88f0-57e77a7502d7": {"questionType": 2, "answer": [ "testBlanksAnswerRevisionE answer one", "testBlanksAnswerRevisionE answer two" ]} }', 3, '2014-08-09 14:01:19.545-04');


/* testBlanksWorkL -> userE -> testBlanksTaskP */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('f92938721eb04523869705898ebc8746', '871b525067124e548ab60784cae0bc64', '9a258f444ee84bab855b53966e53ca10', 5, true, 1, '2014-08-12 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* Latest revision */
INSERT INTO question_work (work_id, answers)
VALUES ('f92938721eb04523869705898ebc8746', '{ "67a68639-0172-4948-88f0-57e77a7502d7": {"questionType": 2, "answer": [ "testBlanksAnswerE answer one", "testBlanksAnswerE answer two" ]} }');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('f92938721eb04523869705898ebc8746', '{ "67a68639-0172-4948-88f0-57e77a7502d7": {"questionType": 2, "answer": [ "testBlanksAnswerE answer one", "testBlanksAnswerE answer two" ]} }', 5, '2014-08-14 14:01:19.545-04');

/* Previous revision */
INSERT INTO question_work_answers (work_id, answers, version, created_at)
VALUES ('f92938721eb04523869705898ebc8746', '{ "67a68639-0172-4948-88f0-57e77a7502d7": {"questionType": 2, "answer": [ "testBlanksAnswerRevisionE answer one", "testBlanksAnswerRevisionE answer two" ]} }', 4, '2014-08-12 14:01:19.545-04');


/* testMediaWorkA -> userC -> testMediaTaskA */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('38800ed08c6e482b84e5806c1f86316d', 'f5f984073a0b4ea5952a575886e90586', 'a7121b74eac111e59ce95e5517507c66', 3, false, 2, '2014-08-12 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* Latest revision */
INSERT INTO media_work (work_id, file_data)
VALUES ('38800ed08c6e482b84e5806c1f86316d', '{ "mediaType": 3, "fileName": "image.jpg" }');

/* Previous revision */
INSERT INTO media_work_data (work_id, file_data, version, created_at)
VALUES ('38800ed08c6e482b84e5806c1f86316d', '{ "mediaType": 3, "fileName": "image.jpg" }', 3, '2014-08-14 14:01:19.545-04');

/* Previous revision */
INSERT INTO media_work_data (work_id, file_data, version, created_at)
VALUES ('38800ed08c6e482b84e5806c1f86316d', '{ "mediaType": 2, "fileName": "video.mp4" }', 2, '2014-08-12 14:01:19.545-04');


/* ---------------------- COMPONENTS ---------------------- */

/* testTextComponentA -> userA (teacher) */
INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, ord, created_at, updated_at)
VALUES ('8cfc608981294c2e9ed145d38077d438', 1, '36c8c0ca50aa4806afa5916a5e33a81f', 'testTextComponentA title', 'testTextComponentA questions', 'testTextComponentA thingsToThinkAbout', 'text', 1, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

INSERT INTO text_components (component_id, content)
VALUES ('8cfc608981294c2e9ed145d38077d438', 'testTextComponentA content');

/* testVideoComponentB -> userA (teacher) */
INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, ord, created_at, updated_at)
VALUES ('50d07485f33c47559ccf59d823cbb79e', 2, '36c8c0ca50aa4806afa5916a5e33a81f', 'testVideoComponentB title', 'testVideoComponentB questions', 'testVideoComponentB thingsToThinkAbout', 'video', 2, '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

INSERT INTO video_components (component_id, vimeo_id, width, height)
VALUES ('50d07485f33c47559ccf59d823cbb79e', '19579282', 640, 480);

/* testAudioComponentC -> userA (teacher) */
INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, ord, created_at, updated_at)
VALUES ('a51c6b535180416daa771cc620dee9c0', 3, '36c8c0ca50aa4806afa5916a5e33a81f', 'testAudioComponentC title', 'testAudioComponentC questions', 'testAudioComponentC thingsToThinkAbout', 'audio', 3, '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

INSERT INTO audio_components (component_id, soundcloud_id)
VALUES ('a51c6b535180416daa771cc620dee9c0', 'dj-whisky-ft-nozipho-just');

/* testAudioComponentE -> userB (teacher) */
INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, ord, created_at, updated_at)
VALUES ('9f2dd973397b4f559618b0ff3af69ecb', 4, '6c0e29bdd05b4b2981156be93e936c59', 'testAudioComponentE title', 'testAudioComponentE questions', 'testAudioComponentE thingsToThinkAbout', 'audio', 4,  '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

INSERT INTO audio_components (component_id, soundcloud_id)
VALUES ('9f2dd973397b4f559618b0ff3af69ecb', 'revolution-radio-network');

INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, ord, created_at, updated_at)
VALUES ('8cfc608981294c2e9ed145d38077d440', 5, '36c8c0ca50aa4806afa5916a5e33a81f', 'testGenericComponentH title', 'testGenericComponentH questions', 'testGenericComponentH thingsToThinkAbout', 'generic_html', 5, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

INSERT INTO generic_html_components (component_id, html_content)
VALUES ('8cfc608981294c2e9ed145d38077d440', 'testGenericComponentH content');


/* ---------------------- PARTS_COMPONENTS ---------------------- */

/* testTextComponentA -> PartA -> project A -> course A -> user A (teacher)*/
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('8cfc608981294c2e9ed145d38077d438', '5cd214be6bba47fa9f350eb8bafec397', '2014-08-01 14:01:19.545-04');

/* testTextComponentA -> PartB -> project A -> course A -> user A (teacher)*/
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('8cfc608981294c2e9ed145d38077d438', 'abb84847a3d247a0ae7d8ce04063afc7', '2014-08-02 14:01:19.545-04');

/* testVideoComponentB -> PartB -> project A -> course A -> user A (teacher)*/
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('50d07485f33c47559ccf59d823cbb79e', 'abb84847a3d247a0ae7d8ce04063afc7', '2014-08-03 14:01:19.545-04');

/* testAudioComponentC -> PartA -> project A -> course A -> user A (teacher)*/
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('a51c6b535180416daa771cc620dee9c0', '5cd214be6bba47fa9f350eb8bafec397', '2014-08-04 14:01:19.545-04');

/* testAudioComponentE -> PartC -> project B -> course B -> user B (teacher)*/
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('9f2dd973397b4f559618b0ff3af69ecb', 'fb01f11b7f2341c8877b68410be62aa5', '2014-08-04 14:01:19.545-04');


/* ---------------------- SCHEDULES ---------------------- */

/* CourseSchedule A -> Course A */
INSERT INTO course_schedules (id, course_id, version, day, start_time, end_time, description, created_at, updated_at)
VALUES ('308792b27a2943c8ad51a5c4f306cdaf', '217c5622ff9e43728e6a95fb3bae300b', 1, '2015-01-15', '2014-08-15 14:01:19.000-04', '2014-08-15 15:01:19.000-04', 'test CourseSchedule A description', '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* CourseSchedule B -> Course B */
INSERT INTO course_schedules (id, course_id, version, day, start_time, end_time, description, created_at, updated_at)
VALUES ('dc1190c2b5fd4bac95fa7d67e1f1d445', '404c800a53854e6b867e365a1e6b00de', 2, '2015-01-16', '2014-08-16 16:01:19.000-04', '2014-08-16 17:01:19.000-04', 'test CourseSchedule B description', '2014-08-04 14:01:19.545-04','2014-08-05 14:01:19.545-04');

/* CourseSchedule C -> Course B */
INSERT INTO course_schedules (id, course_id, version, day, start_time, end_time, description, created_at, updated_at)
VALUES ('6df9d164b1514c389acd6b91301a199d', '404c800a53854e6b867e365a1e6b00de', 3, '2015-01-17', '2014-08-17 18:01:19.000-04', '2014-08-17 19:01:19.000-04', 'test CourseSchedule C description', '2014-08-06 14:01:19.545-04','2014-08-07 14:01:19.545-04');


/* ---------------------- SCHEDULE_EXCEPTIONS ---------------------- */

/* SectionScheduleException A -> UserC -> CourseA */
INSERT INTO course_schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, block, created_at, updated_at)
VALUES ('da17e24aa5454d7494e1427896e13ebe', 'f5f984073a0b4ea5952a575886e90586', '217c5622ff9e43728e6a95fb3bae300b', 1, '2014-08-01', '2014-08-01 14:01:19.000-04', '2014-08-01 15:01:19.000-04', 'testCourseScheduleExceptionA reason', false, '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* SectionScheduleException B -> UserC -> CourseB */
INSERT INTO course_schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, block, created_at, updated_at)
VALUES ('3a285f0c66d041b2851bcfcd203550d9', 'f5f984073a0b4ea5952a575886e90586', '404c800a53854e6b867e365a1e6b00de', 2, '2014-08-02', '2014-08-02 16:01:19.000-04', '2014-08-02 17:01:19.000-04', 'testCourseScheduleExceptionB reason', false, '2014-08-04 14:01:19.545-04','2014-08-05 14:01:19.545-04');

/* SectionScheduleException C -> UserE -> CourseB */
INSERT INTO course_schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, block, created_at, updated_at)
VALUES ('4d7ca313f2164f5985ae88bcbca70317', '871b525067124e548ab60784cae0bc64', '404c800a53854e6b867e365a1e6b00de', 3, '2014-08-03', '2014-08-03 18:01:19.000-04', '2014-08-03 19:01:19.000-04', 'testCourseScheduleExceptionC reason', false, '2014-08-06 14:01:19.545-04','2014-08-07 14:01:19.545-04');

/* SectionScheduleException D -> UserE -> CourseB */
INSERT INTO course_schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, block, created_at, updated_at)
VALUES ('b9a1cd293c04450e9b4a2a63a6871c35', '871b525067124e548ab60784cae0bc64', '404c800a53854e6b867e365a1e6b00de', 4, '2014-08-04', '2014-08-04 20:01:19.000-04', '2014-08-04 21:01:19.000-04', 'testCourseScheduleExceptionD reason', false, '2014-08-08 14:01:19.545-04','2014-08-09 14:01:19.545-04');


/* ---------------------- JOURNAL ---------------------- */

/* JournalEntryA -> userA -> projectA */
INSERT INTO journal_201401 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('6aabd410735f4023ae049f67f84a3846', 1, '36c8c0ca50aa4806afa5916a5e33a81f', 'c9b4cfceaed448fd94f5c980763dfddc', 'view', 'item 1', '2014-01-01 14:01:19.545-04','2014-01-02 14:01:19.545-04');

/* JournalEntryB -> userA -> projectA */
INSERT INTO journal_201402 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('2ec1c7979a604b11a860259ae0f59134', 2, '36c8c0ca50aa4806afa5916a5e33a81f', 'c9b4cfceaed448fd94f5c980763dfddc', 'click', 'item 2', '2014-02-03 14:01:19.545-04','2014-02-04 14:01:19.545-04');

/* JournalEntryC -> userA -> projectA */
INSERT INTO journal_201403 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('f7a218444c764711ae135e8a3758cefb', 3, '36c8c0ca50aa4806afa5916a5e33a81f', 'c9b4cfceaed448fd94f5c980763dfddc', 'watch', 'item 3', '2014-03-05 14:01:19.545-04','2014-03-06 14:01:19.545-04');

/* JournalEntryD -> userA -> projectA */
INSERT INTO journal_201404 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('d77a1706e2304798853f257cad2ed627', 4, '36c8c0ca50aa4806afa5916a5e33a81f', 'c9b4cfceaed448fd94f5c980763dfddc', 'listen', 'item 4', '2014-04-07 14:01:19.545-04','2014-04-08 14:01:19.545-04');

/* JournalEntryE -> userB -> projectB */
INSERT INTO journal_201405 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('0d809bb8779b4e55817cf995959ff290', 5, '6c0e29bdd05b4b2981156be93e936c59', 'e4ae3b9098714339b05c8d39e3aaf65d', 'write', 'item 5', '2014-05-09 14:01:19.545-04','2014-05-10 14:01:19.545-04');

/* JournalEntryF -> userB -> projectB */
INSERT INTO journal_201406 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('594021024fb14fada0e1605b37e09965', 6, '6c0e29bdd05b4b2981156be93e936c59', 'e4ae3b9098714339b05c8d39e3aaf65d', 'create', 'item 6', '2014-06-11 14:01:19.545-04','2014-06-12 14:01:19.545-04');

/* JournalEntryG -> userB -> projectB */
INSERT INTO journal_201407 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('eab8e5d1d88c4718b1bfda524daca133', 7, '6c0e29bdd05b4b2981156be93e936c59', 'e4ae3b9098714339b05c8d39e3aaf65d', 'update', 'item 7', '2014-07-13 14:01:19.545-04','2014-07-14 14:01:19.545-04');

/* JournalEntryH -> userB -> projectB */
INSERT INTO journal_201408 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('453c7d392bbd40419fb46fb59a134395', 8, '6c0e29bdd05b4b2981156be93e936c59', 'e4ae3b9098714339b05c8d39e3aaf65d', 'delete', 'item 8', '2014-08-15 14:01:19.545-04','2014-08-16 14:01:19.545-04');

/* JournalEntryI -> userA -> projectA */
INSERT INTO journal_201409 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('cc19d1cd9114413a96ba46c981525e30', 9, '36c8c0ca50aa4806afa5916a5e33a81f', 'c9b4cfceaed448fd94f5c980763dfddc', 'delete', 'item 9', '2014-09-17 14:01:19.545-04','2014-09-18 14:01:19.545-04');


/* ---------------------- CHAT_LOGS ---------------------- */

/* chatA */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('217c5622ff9e43728e6a95fb3bae300b', 1, '871b525067124e548ab60784cae0bc64', 'testChatA message', false, '2014-08-01 14:01:19.545-04');

/* chatB */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('217c5622ff9e43728e6a95fb3bae300b', 2, 'f5f984073a0b4ea5952a575886e90586', 'testChatB message', false, '2014-08-02 14:01:19.545-04');

/* chatC */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('217c5622ff9e43728e6a95fb3bae300b', 3, 'f5f984073a0b4ea5952a575886e90586', 'testChatC message', false, '2014-08-03 14:01:19.545-04');

/* chatD */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('217c5622ff9e43728e6a95fb3bae300b', 4, '871b525067124e548ab60784cae0bc64', 'testChatD message', false, '2014-08-04 14:01:19.545-04');

/* chatE */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('217c5622ff9e43728e6a95fb3bae300b', 5, 'f5f984073a0b4ea5952a575886e90586', 'testChatE message', false, '2014-08-05 14:01:19.545-04');

/* chatF */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('217c5622ff9e43728e6a95fb3bae300b', 6, 'f5f984073a0b4ea5952a575886e90586', 'testChatF message', false, '2014-08-06 14:01:19.545-04');


/* -----------------------  ProjectScratchpads  ----------------------- */

/* projectScratchpadA  user a project a document j*/
INSERT INTO project_notes (user_id, project_id, document_id)
VALUES ('36c8c0ca50aa4806afa5916a5e33a81f', 'c9b4cfceaed448fd94f5c980763dfddc', '30739c6d43774a2f8aa3d1240dfb0740');

/* taskScratchpadB  user a project b document f*/
INSERT INTO project_notes (user_id, project_id, document_id)
VALUES ('36c8c0ca50aa4806afa5916a5e33a81f', 'e4ae3b9098714339b05c8d39e3aaf65d', '1a9d5407b3c444a18e7e1d7e9578eabc');

/* taskScratchpadC */
INSERT INTO project_notes (user_id, project_id, document_id)
VALUES ('f5f984073a0b4ea5952a575886e90586', 'e4ae3b9098714339b05c8d39e3aaf65d', '196a1793c6884f66b725a8353dd1ac67');

/* taskScratchpadD */
INSERT INTO project_notes (user_id, project_id, document_id)
VALUES ('871b525067124e548ab60784cae0bc64', 'e4ae3b9098714339b05c8d39e3aaf65d', '78b9baaf16b743a39cec410104cdde4e');


/* -----------------------  User tokens  ----------------------- */

/* activation */
INSERT INTO user_tokens (user_id, nonce, token_type, created_at)
VALUES ('8b6dc674-d1ae-11e5-9080-08626681851d', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'activation', '2014-08-01 14:01:19.545-04');

/* password reset */
INSERT INTO user_tokens (user_id, nonce, token_type, created_at)
VALUES ('7c62a526-d1b0-11e5-9080-08626681851d', '$s0$100801$Im7kWa5XcOMHIilt7A==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'password_reset', '2014-08-01 14:01:19.545-04');


/* -----------------------  WORDS  ----------------------- */

INSERT INTO words(word, lang)
VALUES('bisexualpotato', 'en');

INSERT INTO words(word, lang)
VALUES('cielbleu', 'fr');

INSERT INTO words(word, lang)
VALUES('omniscienttable', 'en');

INSERT INTO words(word, lang)
VALUES('alientea', 'en');

INSERT INTO words(word, lang)
VALUES('vinrouge', 'fr');

INSERT INTO words(word, lang)
VALUES ('интереснаяличность', 'ru');


/* -----------------------  LINKS  ----------------------- */

/* course A */
INSERT INTO links(course_id, link, created_at)
VALUES('217c5622ff9e43728e6a95fb3bae300b', 'bisexualpotato',  '2014-08-05 14:01:19.545-04');
/* course b */
INSERT INTO links(course_id, link, created_at)
VALUES  ('404c800a53854e6b867e365a1e6b00de', 'vinrouge', '2014-08-05 14:01:19.545-04');
