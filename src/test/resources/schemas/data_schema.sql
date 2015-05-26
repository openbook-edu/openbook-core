/* ---------------------- USERS ---------------------- */

/* user A */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', 1, 'testUserA@example.com', 'testUserA', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestA', 'UserA', '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

/* user B */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', 2, 'testUserB@example.com', 'testUserB', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestB', 'UserB', '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

/* user C */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', 3, 'testUserC@example.com', 'testUserC', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestC', 'UserC', '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

/* user E (student) */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x871b525067124e548ab60784cae0bc64', 4, 'testUserE@example.com', 'testUserE', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestE', 'UserE', '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

/* user F */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x4d01347ec5924e5fb09fdd281b3d9b87', 5, 'testUserF@example.com', 'testUserF', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestF', 'UserF', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* user G */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\xc4d948967e1b45fabae74fb3a89a4d63', 6, 'testUserG@example.com', 'testUserG', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestG', 'UserG', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* user H no references in other tables */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x5099a6b48809400d8e380119184d0f93', 7, 'testUserH@example.com', 'testUserH', '$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=', 'TestH', 'UserH', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');


/* ---------------------- ROLES ---------------------- */

/* role A */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('\x1430e95077f94b30baf8bb226fc7091a', 1, 'test role A', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* role B */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('\xa011504cd11840cdb9eb6e10d5738c67', 2, 'test role B', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* role C */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('\x31a4c2e6762a4303bbb8e64c24048920', 3, 'test role C', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* role F */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('\x45b3552707ad4c4f9051f0e755216163', 4, 'test role F', '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

/* role G */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('\x45cc7cc8987645f58efeccd4dba7ea69', 5, 'test role G', '2014-08-17 14:01:19.545-04', '2014-08-18 14:01:19.545-04');

/* role H */
INSERT INTO roles (id, version, name, created_at, updated_at)
VALUES ('\x2a3edf38750a46aa84289fb08e648ee8', 6, 'test role H', '2014-08-19 14:01:19.545-04', '2014-08-20 14:01:19.545-04');


/* ---------------------- USERS_ROLES ---------------------- */

/* UserA -> RoleA */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', '\x1430e95077f94b30baf8bb226fc7091a', '2014-08-13 14:01:19.545-04');

/* UserA -> RoleB */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', '\xa011504cd11840cdb9eb6e10d5738c67', '2014-08-14 14:01:19.545-04');

/* UserA -> RoleF */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', '\x45b3552707ad4c4f9051f0e755216163', '2014-08-14 14:01:19.545-04');

/* UserA -> RoleG */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', '\x45cc7cc8987645f58efeccd4dba7ea69', '2014-08-14 14:01:19.545-04');

/* UserB -> RoleA */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\x1430e95077f94b30baf8bb226fc7091a', '2014-08-15 14:01:19.545-04');

/* UserB -> RoleB */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\xa011504cd11840cdb9eb6e10d5738c67', '2014-08-16 14:01:19.545-04');

/* UserB -> RoleF */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\x45b3552707ad4c4f9051f0e755216163', '2014-08-17 14:01:19.545-04');

/* UserF -> RoleC */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x4d01347ec5924e5fb09fdd281b3d9b87', '\x31a4c2e6762a4303bbb8e64c24048920', '2014-08-19 14:01:19.545-04');


/* ---------------------- COURSES ---------------------- */

/* course A -> user A (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, chat_enabled, created_at, updated_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 1, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'test course A', 1574408, 'test course A slug', true, '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* course B -> user B (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, chat_enabled, created_at, updated_at)
VALUES ('\x404c800a53854e6b867e365a1e6b00de', 2, '\x6c0e29bdd05b4b2981156be93e936c59', 'test course B', 2230288, 'test course B slug', true, '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* course D -> user F (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, chat_enabled, created_at, updated_at)
VALUES ('\x94cc65bb45424f628e08d58522e7b5f1', 3, '\x4d01347ec5924e5fb09fdd281b3d9b87', 'test course D', 269368, 'test course D slug', true, '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* course F -> user F (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, chat_enabled, created_at, updated_at)
VALUES ('\x287b61f5da6b4de785353bc500cffac7', 4, '\x4d01347ec5924e5fb09fdd281b3d9b87', 'test course F', 269368, 'test course F slug', true, '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

/* course G -> user F (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, slug, chat_enabled, created_at, updated_at)
VALUES ('\xb24abba8e6c74700900ce66ed0185a70', 5, '\x4d01347ec5924e5fb09fdd281b3d9b87', 'test course G', 1508909, 'test course G slug', true, '2014-08-17 14:01:19.545-04', '2014-08-18 14:01:19.545-04');


/* ---------------------- USERS_COURSES ---------------------- */

/* UserC (student) -> CourseA -> user A (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\x217c5622ff9e43728e6a95fb3bae300b', '2014-08-05 14:01:19.545-04');

/* UserC (student) -> CourseB -> user B (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\x404c800a53854e6b867e365a1e6b00de', '2014-08-06 14:01:19.545-04');

/* UserE (student) -> CourseB -> user B (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\x871b525067124e548ab60784cae0bc64', '\x404c800a53854e6b867e365a1e6b00de', '2014-08-07 14:01:19.545-04');

/* UserE (student) -> CourseA -> user A (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\x871b525067124e548ab60784cae0bc64', '\x217c5622ff9e43728e6a95fb3bae300b', '2014-08-11 14:01:19.545-04');

/* UserG (student) -> CourseF -> user F (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\xc4d948967e1b45fabae74fb3a89a4d63', '\x287b61f5da6b4de785353bc500cffac7', '2014-08-08 14:01:19.545-04');

/* UserH (student) -> CourseF -> user F (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\x5099a6b48809400d8e380119184d0f93', '\x287b61f5da6b4de785353bc500cffac7', '2014-08-10 14:01:19.545-04');

/* UserH (student) -> CourseD -> user F (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\x5099a6b48809400d8e380119184d0f93', '\x94cc65bb45424f628e08d58522e7b5f1', '2014-08-10 14:01:19.545-04');


/* ---------------------- PROJECTS ---------------------- */

/* project A -> course A -> user A (teacher) */
INSERT INTO projects (id, course_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('\xc9b4cfceaed448fd94f5c980763dfddc', '\x217c5622ff9e43728e6a95fb3bae300b', 1, 'test project A', 'test project slug A', 'test project A description', 'any', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* project B -> course B -> user B (teacher) */
INSERT INTO projects (id, course_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('\xe4ae3b9098714339b05c8d39e3aaf65d', '\x404c800a53854e6b867e365a1e6b00de', 2, 'test project B', 'test project slug B', 'test project B description', 'free', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* project C -> course B -> user B (teacher) */
INSERT INTO projects (id, course_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('\x4ac4d872451b4092b13f643d6d5fa930', '\x404c800a53854e6b867e365a1e6b00de', 3, 'test project C', 'test project slug C', 'test project C description', 'course', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* project E -> course A -> user A (teacher) */
INSERT INTO projects (id, course_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('\xb36919cb2df043b7bb7f36cae797deaa', '\x217c5622ff9e43728e6a95fb3bae300b', 4, 'test project E', 'test project slug E', 'test project E description', 'course', '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');


/* ---------------------- PARTS ---------------------- */

/* part A -> project A -> course A -> user A (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\x5cd214be6bba47fa9f350eb8bafec397', 1, '\xc9b4cfceaed448fd94f5c980763dfddc', 'test part A', true, 10, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

/* part B -> project A -> course A -> user A (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\xabb84847a3d247a0ae7d8ce04063afc7', 2, '\xc9b4cfceaed448fd94f5c980763dfddc', 'test part B', false, 11, '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

/* part C -> project B -> course B -> user B (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\xfb01f11b7f2341c8877b68410be62aa5', 3, '\xe4ae3b9098714339b05c8d39e3aaf65d', 'test part C', true, 12, '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

/* part E -> project C -> course B -> user B (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\xc850ec53f0a9460d918a5e6fd538f376', 4, '\x4ac4d872451b4092b13f643d6d5fa930', 'test part E', false, 13, '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

/* part F -> project C -> course B -> user B (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\xe8d526846afd48e58049a179e8868432', 5, '\x4ac4d872451b4092b13f643d6d5fa930', 'test part F', true, 14, '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* part G -> project A -> course A -> user A (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\x8e080c002b204e7bb18c2582d79e7e68', 6, '\xc9b4cfceaed448fd94f5c980763dfddc', 'test part G', true, 15, '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* part H -> project C -> course B -> user B (teacher) */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\x45a146b3fd9a4cab9d1d3e9b0b15e12c', 7, '\x4ac4d872451b4092b13f643d6d5fa930', 'test part H', true, 16, '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');


/* ---------------------- TASKS ---------------------- */

/* longAnswerTask A -> part A -> project A -> course A -> user A (teacher)*/
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, response_title, notes_title, created_at, updated_at)
VALUES ('\xbf1a6ed09f834cb485c1ad456299b3a3', 1, '\x5cd214be6bba47fa9f350eb8bafec397', null, 'test longAnswerTask A', 'test longAnswerTask A description', 10, 0, true, 'test longAnswerTask A response title', 'test longAnswerTask A notes title', '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

INSERT INTO long_answer_tasks (task_id)
VALUES ('\xbf1a6ed09f834cb485c1ad456299b3a3');


/* shortAnswerTask B -> part A (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, response_title, notes_title, created_at, updated_at)
VALUES ('\x10ef05ee7b494352b86e70510adf617f', 2, '\x5cd214be6bba47fa9f350eb8bafec397', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test shortAnswerTask B', 'test shortAnswerTask B description', 11, 1, true, 'test shortAnswerTask B response title', 'test shortAnswerTask B notes title', '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

INSERT INTO short_answer_tasks (task_id, max_length)
VALUES ('\x10ef05ee7b494352b86e70510adf617f', 51);


/* multipleChoiceTask C -> part A (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, response_title, notes_title, created_at, updated_at)
VALUES ('\x76cc2ed7611b4dafaa3f20efe42a65a0', 3, '\x5cd214be6bba47fa9f350eb8bafec397', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test MultipleChoiceTask C', 'test MultipleChoiceTask C description', 12, 2, true, 'test MultipleChoiceTask C response title', 'test MultipleChoiceTask C notes title', '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

INSERT INTO multiple_choice_tasks (task_id, choices, answers, allow_multiple, randomize)
VALUES ('\x76cc2ed7611b4dafaa3f20efe42a65a0', '{choice 1, choice 2}', '{1, 2}', false, true);


/* orderingTask D -> part B (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, response_title, notes_title, created_at, updated_at)
VALUES ('\x808400838923476fa8738ba6c55e30c8', 4, '\xabb84847a3d247a0ae7d8ce04063afc7', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test OrderingTask D', 'test OrderingTask D description', 13, 3, true, 'test OrderingTask D response title', 'test OrderingTask D notes title', '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

INSERT INTO ordering_tasks (task_id, elements, answers, randomize)
VALUES ('\x808400838923476fa8738ba6c55e30c8', '{element 3, element 4}', '{3, 4}', true);


/* orderingTask L -> part B (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, response_title, notes_title, created_at, updated_at)
VALUES ('\x3d3578bd60d34aeabe070359dad2fecb', 6, '\xabb84847a3d247a0ae7d8ce04063afc7', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test OrderingTask L', 'test OrderingTask L description', 17, 3, true, 'test OrderingTask L response title', 'test OrderingTask L notes title', '2014-08-08 14:01:19.545-04', '2014-08-09 14:01:19.545-04');

INSERT INTO ordering_tasks (task_id, elements, answers, randomize)
VALUES ('\x3d3578bd60d34aeabe070359dad2fecb', '{element 5, element 6}', '{5, 6}', true);


/* orderingTask N -> part G (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, response_title, notes_title, created_at, updated_at)
VALUES ('\x599a78ad5bff4246983532fcb41168a6', 7, '\x8e080c002b204e7bb18c2582d79e7e68', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test OrderingTask N', 'test OrderingTask N description', 18, 3, true, 'test OrderingTask N response title', 'test OrderingTask N notes title', '2014-08-10 14:01:19.545-04', '2014-08-11 14:01:19.545-04');

INSERT INTO ordering_tasks (task_id, elements, answers, randomize)
VALUES ('\x599a78ad5bff4246983532fcb41168a6', '{element 6, element 7}', '{6, 7}', true);


/* matchingTask E -> part C (dependency_id -> longAnswerTask A) -> project B -> course B -> user B (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, response_title, notes_title, created_at, updated_at)
VALUES ('\x468a35bfbaf84045aa184688f4d0721f', 5, '\xfb01f11b7f2341c8877b68410be62aa5', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test MatchingTask E', 'test MatchingTask E description', 14, 4, true, 'test MatchingTask E response title', 'test MatchingTask E notes title', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

INSERT INTO matching_tasks (task_id, elements_left, elements_right, answers, randomize)
VALUES ('\x468a35bfbaf84045aa184688f4d0721f', '{choice left 5, choice left 6}', '{choice right 7, choice right 8}', '{{5, 6}, {7, 8}}', true);


/* matchingTask K -> partB (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, response_title, notes_title, created_at, updated_at)
VALUES ('\x337fa73136854ba38668280c0096514c', 7, '\xabb84847a3d247a0ae7d8ce04063afc7', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test MatchingTask K', 'test MatchingTask K description', 16, 4, true, 'test MatchingTask K response title', 'test MatchingTask K notes title', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

INSERT INTO matching_tasks (task_id, elements_left, elements_right, answers, randomize)
VALUES ('\x337fa73136854ba38668280c0096514c', '{choice left 6, choice left 7}', '{choice right 8, choice right 9}', '{{6, 7}, {8, 9}}', true);


/* matchingTask M -> partB (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, response_title, notes_title, created_at, updated_at)
VALUES ('\x129f2b0856d34e14aa5b659f53f71e39', 8, '\xabb84847a3d247a0ae7d8ce04063afc7', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test MatchingTask M', 'test MatchingTask M description', 17, 4, true, 'test MatchingTask M response title', 'test MatchingTask M notes title', '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

INSERT INTO matching_tasks (task_id, elements_left, elements_right, answers, randomize)
VALUES ('\x129f2b0856d34e14aa5b659f53f71e39', '{choice left 7, choice left 8}', '{choice right 9, choice right 10}', '{{7, 8}, {9, 10}}', true);


/* ---------------------- DOCUMENTS ---------------------- */

/* documentA -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\xfd923b3f6dc2472e8ce77a8fcc6a1a20', 2, '\xf5f984073a0b4ea5952a575886e90586', 'testDocumentA title', '{"ops":[{"insert":"Hello Sam"}]}', '2014-08-01 14:01:19.545-04', '2014-08-03 14:01:19.545-04');

/* documentB -> userE */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x15173757b881444082854e3d2c03616a', 2, '\x871b525067124e548ab60784cae0bc64', 'testDocumentB title', '{"ops":[{"insert":"Hello Dean"}]}', '2014-08-03 14:01:19.545-04', '2014-08-05 14:01:19.545-04');

/* documentC -> userE */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x462b7f6c8b624c998643a63b2720b2a7', 2, '\x871b525067124e548ab60784cae0bc64', 'testDocumentC title', '{"ops":[{"insert":"Hello Jhonatan"}]}', '2014-08-03 14:01:19.545-04', '2014-08-05 14:01:19.545-04');

/* documentD -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\xbd01c98803694ddaada205a9ff3645cf', 2, '\xf5f984073a0b4ea5952a575886e90586', 'testDocumentD title', '{"ops":[{"insert":"Hello Morgan"}]}', '2014-08-05 14:01:19.545-04', '2014-08-07 14:01:19.545-04');

/* documentF -> userA (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x1a9d5407b3c444a18e7e1d7e9578eabc', 2, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'testDocumentF title', '{"ops":[{"insert":"Hello Jason"}]}', '2014-08-09 14:01:19.545-04', '2014-08-11 14:01:19.545-04');

/* documentG -> userA (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x300ddfb7f9bf47fea0b226f332828fff', 2, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'testDocumentG title', '{"ops":[{"insert":"Hello Moony"}]}', '2014-08-11 14:01:19.545-04', '2014-08-13 14:01:19.545-04');

/* documentH -> userB (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\xeb8ef353d22f48a4a356351e0de3ed16', 2, '\x6c0e29bdd05b4b2981156be93e936c59', 'testDocumentH title', '{"ops":[{"insert":"Hello Flipper"}]}', '2014-08-13 14:01:19.545-04', '2014-08-15 14:01:19.545-04');

/* documentI -> userB (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x9110c16f45fd42119e39b15ab8b6f9ee', 2, '\x6c0e29bdd05b4b2981156be93e936c59', 'testDocumentI title', '{"ops":[{"insert":"Hello Groovy"}]}', '2014-08-15 14:01:19.545-04', '2014-08-17 14:01:19.545-04');

/* documentJ -> userA (teacher) */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x30739c6d43774a2f8aa3d1240dfb0740', 2, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'testDocumentJ title', '{"ops":[{"insert":"Hello Bobby"}]}', '2014-08-17 14:01:19.545-04', '2014-08-19 14:01:19.545-04');

/* documentK -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x2f1180f017f4488b9f03ad8fbfbeaf3a', 2, '\xf5f984073a0b4ea5952a575886e90586', 'testDocumentK title', '{"ops":[{"insert":"Hello Moris"}]}', '2014-08-19 14:01:19.545-04', '2014-08-21 14:01:19.545-04');

/* documentL -> userE */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x7c9d0daefe794eccb36cc141a4122fab', 2, '\x871b525067124e548ab60784cae0bc64', 'testDocumentL title', '{"ops":[{"insert":"Hello Boris"}]}', '2014-08-21 14:01:19.545-04', '2014-08-23 14:01:19.545-04');

/* documentM -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x0ed856aafd4c486db6c5293ca18c37dd', 2, '\xf5f984073a0b4ea5952a575886e90586', 'testDocumentM title', '{"ops":[{"insert":"Hello Vasea"}]}', '2014-08-23 14:01:19.545-04', '2014-08-25 14:01:19.545-04');

/* documentN -> userC */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x196a1793c6884f66b725a8353dd1ac67', 2, '\xf5f984073a0b4ea5952a575886e90586', 'testDocumentN title', '{"ops":[{"insert":"Hello Petea"}]}', '2014-08-25 14:01:19.545-04', '2014-08-27 14:01:19.545-04');

/* documentO -> userE */
INSERT INTO documents (id, version, owner_id, title, delta, created_at, updated_at)
VALUES ('\x78b9baaf16b743a39cec410104cdde4e', 2, '\x871b525067124e548ab60784cae0bc64', 'testDocumentO title', '{"ops":[{"insert":"Hello Doris"}]}', '2014-08-27 14:01:19.545-04', '2014-08-29 14:01:19.545-04');


/* ---------------------- DOCUMENT_REVISIONS ---------------------- */

/* CurrentRevisionA */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('\xfd923b3f6dc2472e8ce77a8fcc6a1a20', 2, '\xf5f984073a0b4ea5952a575886e90586', '{"ops":[{"delete":7},{"insert":"Hello"},{"retain":4}]}', '2014-08-03 14:01:19.545-04');

/* PreviousRevisionA */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('\xfd923b3f6dc2472e8ce77a8fcc6a1a20', 1, '\xf5f984073a0b4ea5952a575886e90586', '{"ops":[{"insert":"Goodbye Sam"}]}', '2014-08-02 14:01:19.545-04');

/* CurrentRevisionB */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('\x15173757b881444082854e3d2c03616a', 2, '\x871b525067124e548ab60784cae0bc64', '{"ops":[{"delete":7},{"insert":"Hello"},{"retain":5}]}', '2014-08-05 14:01:19.545-04');

/* PreviousRevisionB */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('\x15173757b881444082854e3d2c03616a', 1, '\x871b525067124e548ab60784cae0bc64', '{"ops":[{"insert":"Goodbye Dean"}]}', '2014-08-04 14:01:19.545-04');

/* CurrentRevisionC */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('\x462b7f6c8b624c998643a63b2720b2a7', 2, '\x871b525067124e548ab60784cae0bc64', '{"ops":[{"delete":7},{"insert":"Hello"},{"retain":9}]}', '2014-08-05 14:01:19.545-04');

/* PreviousRevisionC */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('\x462b7f6c8b624c998643a63b2720b2a7', 1, '\x871b525067124e548ab60784cae0bc64', '{"ops":[{"insert":"Goodbye Jhonatan"}]}', '2014-08-04 14:01:19.545-04');

/* CurrentRevisionD */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('\xbd01c98803694ddaada205a9ff3645cf', 2, '\xf5f984073a0b4ea5952a575886e90586', '{"ops":[{"delete":7},{"insert":"Hello"},{"retain":7}]}', '2014-08-07 14:01:19.545-04');

/* PreviousRevisionD */
INSERT INTO document_revisions (document_id, version, author_id, delta, created_at)
VALUES ('\xbd01c98803694ddaada205a9ff3645cf', 1, '\xf5f984073a0b4ea5952a575886e90586', '{"ops":[{"insert":"Goodbye Morgan"}]}', '2014-08-06 14:01:19.545-04');


/* ---------------------- TASK_FEEDBACKS ---------------------- */

/* taskFeedbackA */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\xbf1a6ed09f834cb485c1ad456299b3a3', '\x1a9d5407b3c444a18e7e1d7e9578eabc');

/* taskFeedbackB */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('\x871b525067124e548ab60784cae0bc64', '\x10ef05ee7b494352b86e70510adf617f', '\x300ddfb7f9bf47fea0b226f332828fff');

/* taskFeedbackC */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\x468a35bfbaf84045aa184688f4d0721f', '\xeb8ef353d22f48a4a356351e0de3ed16');

/* taskFeedbackD */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('\x871b525067124e548ab60784cae0bc64', '\x468a35bfbaf84045aa184688f4d0721f', '\x9110c16f45fd42119e39b15ab8b6f9ee');

/* taskFeedbackE */
INSERT INTO task_feedbacks (student_id, task_id, document_id)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\x599a78ad5bff4246983532fcb41168a6', '\x30739c6d43774a2f8aa3d1240dfb0740');


/* ---------------------- TASK_SCRATCHPADS ---------------------- */

/* taskScratchpadA */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\xbf1a6ed09f834cb485c1ad456299b3a3', '\x2f1180f017f4488b9f03ad8fbfbeaf3a');

/* taskScratchpadB */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('\x871b525067124e548ab60784cae0bc64', '\x10ef05ee7b494352b86e70510adf617f', '\x7c9d0daefe794eccb36cc141a4122fab');

/* taskScratchpadC */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\x468a35bfbaf84045aa184688f4d0721f', '\x196a1793c6884f66b725a8353dd1ac67');

/* taskScratchpadD */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('\x871b525067124e548ab60784cae0bc64', '\x468a35bfbaf84045aa184688f4d0721f', '\x78b9baaf16b743a39cec410104cdde4e');

/* taskScratchpadE */
INSERT INTO task_notes (user_id, task_id, document_id)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\x599a78ad5bff4246983532fcb41168a6', '\x0ed856aafd4c486db6c5293ca18c37dd');


/* ----------------------- WORK ---------------------- */

/* longAnswerWorkA -> userC -> longAnswerTaskA -> documentA */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\x441374e20b1643ecadb96a3251081d24', '\xf5f984073a0b4ea5952a575886e90586', '\xbf1a6ed09f834cb485c1ad456299b3a3', 1, true, 0, '2014-08-01 14:01:19.545-04', '2014-08-03 14:01:19.545-04');

INSERT INTO long_answer_work (work_id, document_id)
VALUES ('\x441374e20b1643ecadb96a3251081d24', '\xfd923b3f6dc2472e8ce77a8fcc6a1a20');


/* longAnswerWorkF -> userE -> longAnswerTaskA -> documentB */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\xf7fcffc37b794de7b6ddcf37aa155fd9', '\x871b525067124e548ab60784cae0bc64', '\xbf1a6ed09f834cb485c1ad456299b3a3', 1, true, 0, '2014-08-03 14:01:19.545-04', '2014-08-05 14:01:19.545-04');

INSERT INTO long_answer_work (work_id, document_id)
VALUES ('\xf7fcffc37b794de7b6ddcf37aa155fd9', '\x15173757b881444082854e3d2c03616a');


/* ShortAnswerWorkB -> userE -> shortAnswerTask B -> documentC */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\xcbf452cd915a4b249d0292be013bbba8', '\x871b525067124e548ab60784cae0bc64', '\x10ef05ee7b494352b86e70510adf617f', 1, false, 1, '2014-08-03 14:01:19.545-04', '2014-08-05 14:01:19.545-04');

INSERT INTO short_answer_work (work_id, document_id)
VALUES ('\xcbf452cd915a4b249d0292be013bbba8', '\x462b7f6c8b624c998643a63b2720b2a7');


/* ShortAnswerWorkG -> userC -> shortAnswerTask B -> documentD */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\xb7bb09c16aca40de81525da483a5c476', '\xf5f984073a0b4ea5952a575886e90586', '\x10ef05ee7b494352b86e70510adf617f', 1, false, 1, '2014-08-05 14:01:19.545-04', '2014-08-07 14:01:19.545-04');

INSERT INTO short_answer_work (work_id, document_id)
VALUES ('\xb7bb09c16aca40de81525da483a5c476', '\xbd01c98803694ddaada205a9ff3645cf');


/* MultipleChoiceWorkC -> userC -> multipleChoiceTask C */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\xedfd619897b04f219e15fbe4ed051970', '\xf5f984073a0b4ea5952a575886e90586', '\x76cc2ed7611b4dafaa3f20efe42a65a0', 3, true, 2, '2014-08-05 14:01:19.545-04', '2014-08-07 14:01:19.545-04');

/* Latest revision */
INSERT INTO multiple_choice_work (work_id, version, response, created_at)
VALUES ('\xedfd619897b04f219e15fbe4ed051970', 3, '{1, 2}', '2014-08-07 14:01:19.545-04');

/* Previous revision */
INSERT INTO multiple_choice_work (work_id, version, response, created_at)
VALUES ('\xedfd619897b04f219e15fbe4ed051970', 2, '{3, 4}', '2014-08-06 14:01:19.545-04');


/* MultipleChoiceWorkH -> userE -> multipleChoiceTask C */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\x8f3b9f09db434670b1590763eb4eaecd', '\x871b525067124e548ab60784cae0bc64', '\x76cc2ed7611b4dafaa3f20efe42a65a0', 8, true, 2, '2014-08-07 14:01:19.545-04', '2014-08-09 14:01:19.545-04');

/* Latest revision */
INSERT INTO multiple_choice_work (work_id, version, response, created_at)
VALUES ('\x8f3b9f09db434670b1590763eb4eaecd', 8, '{3, 4}', '2014-08-09 14:01:19.545-04');

/* Previous revision */
INSERT INTO multiple_choice_work (work_id, version, response, created_at)
VALUES ('\x8f3b9f09db434670b1590763eb4eaecd', 7, '{5, 6}', '2014-08-08 14:01:19.545-04');


/* OrderingWorkD -> userC -> orderingTaskN */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\x125eef5a7e89441cb138c1803bafdc03', '\xf5f984073a0b4ea5952a575886e90586', '\x599a78ad5bff4246983532fcb41168a6', 4, true, 3, '2014-08-07 14:01:19.545-04', '2014-08-09 14:01:19.545-04');

/* Latest revision */
INSERT INTO ordering_work (work_id, version, response, created_at)
VALUES ('\x125eef5a7e89441cb138c1803bafdc03', 4, '{3, 4}', '2014-08-09 14:01:19.545-04');

/* Previous revision */
INSERT INTO ordering_work (work_id, version, response, created_at)
VALUES ('\x125eef5a7e89441cb138c1803bafdc03', 3, '{5, 6}', '2014-08-08 14:01:19.545-04');


/* OrderingWorkI -> userE -> orderingTaskN */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\xdb5165f44d4840079191beecd77763c7', '\x871b525067124e548ab60784cae0bc64', '\x599a78ad5bff4246983532fcb41168a6', 5, true, 3, '2014-08-09 14:01:19.545-04', '2014-08-11 14:01:19.545-04');

/* Latest revision */
INSERT INTO ordering_work (work_id, version, response, created_at)
VALUES ('\xdb5165f44d4840079191beecd77763c7', 5, '{4, 5}', '2014-08-11 14:01:19.545-04');

/* Previous revision */
INSERT INTO ordering_work (work_id, version, response, created_at)
VALUES ('\xdb5165f44d4840079191beecd77763c7', 4, '{6, 7}', '2014-08-10 14:01:19.545-04');


/* testMatchingWorkE -> userC -> testMatchingTaskE */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\xe47442dd8ac94d06ad6fef62720d4ed3', '\xf5f984073a0b4ea5952a575886e90586', '\x468a35bfbaf84045aa184688f4d0721f', 5, true, 4, '2014-08-09 14:01:19.545-04', '2014-08-11 14:01:19.545-04');

/* Latest revision */
INSERT INTO matching_work (work_id, version, response, created_at)
VALUES ('\xe47442dd8ac94d06ad6fef62720d4ed3', 5, '{{5, 6}, {7, 8}}', '2014-08-11 14:01:19.545-04');

/* Previous revision */
INSERT INTO matching_work (work_id, version, response, created_at)
VALUES ('\xe47442dd8ac94d06ad6fef62720d4ed3', 4, '{{6, 7}, {8, 9}}', '2014-08-10 14:01:19.545-04');


/* testMatchingWorkJ -> userE -> testMatchingTaskE */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\xc57e033551da41449dfcdfa97f5f1a7c', '\x871b525067124e548ab60784cae0bc64', '\x468a35bfbaf84045aa184688f4d0721f', 6, false, 4, '2014-08-10 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* Latest revision */
INSERT INTO matching_work (work_id, version, response, created_at)
VALUES ('\xc57e033551da41449dfcdfa97f5f1a7c', 6, '{{6, 7}, {8, 9}}', '2014-08-12 14:01:19.545-04');

/* Previous revision */
INSERT INTO matching_work (work_id, version, response, created_at)
VALUES ('\xc57e033551da41449dfcdfa97f5f1a7c', 5, '{{7, 8}, {9, 10}}', '2014-08-11 14:01:19.545-04');


/* ---------------------- COMPONENTS ---------------------- */

/* testTextComponentA -> userA (teacher) */
INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, created_at, updated_at)
VALUES ('\x8cfc608981294c2e9ed145d38077d438', 1, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'testTextComponentA title', 'testTextComponentA questions', 'testTextComponentA thingsToThinkAbout', 'text', '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

INSERT INTO text_components (component_id, content)
VALUES ('\x8cfc608981294c2e9ed145d38077d438', 'testTextComponentA content');

/* testVideoComponentB -> userA (teacher) */
INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, created_at, updated_at)
VALUES ('\x50d07485f33c47559ccf59d823cbb79e', 2, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'testVideoComponentB title', 'testVideoComponentB questions', 'testVideoComponentB thingsToThinkAbout', 'video', '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

INSERT INTO video_components (component_id, vimeo_id, width, height)
VALUES ('\x50d07485f33c47559ccf59d823cbb79e', '19579282', 640, 480);

/* testAudioComponentC -> userA (teacher) */
INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, created_at, updated_at)
VALUES ('\xa51c6b535180416daa771cc620dee9c0', 3, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'testAudioComponentC title', 'testAudioComponentC questions', 'testAudioComponentC thingsToThinkAbout', 'audio', '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

INSERT INTO audio_components (component_id, soundcloud_id)
VALUES ('\xa51c6b535180416daa771cc620dee9c0', 'dj-whisky-ft-nozipho-just');

/* testAudioComponentE -> userB (teacher) */
INSERT INTO components (id, version, owner_id, title, questions, things_to_think_about, type, created_at, updated_at)
VALUES ('\x9f2dd973397b4f559618b0ff3af69ecb', 4, '\x6c0e29bdd05b4b2981156be93e936c59', 'testAudioComponentE title', 'testAudioComponentE questions', 'testAudioComponentE thingsToThinkAbout', 'audio', '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

INSERT INTO audio_components (component_id, soundcloud_id)
VALUES ('\x9f2dd973397b4f559618b0ff3af69ecb', 'revolution-radio-network');


/* ---------------------- PARTS_COMPONENTS ---------------------- */

/* testTextComponentA -> PartA -> project A -> course A -> user A (teacher)*/
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('\x8cfc608981294c2e9ed145d38077d438', '\x5cd214be6bba47fa9f350eb8bafec397', '2014-08-01 14:01:19.545-04');

/* testTextComponentA -> PartB -> project A -> course A -> user A (teacher)*/
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('\x8cfc608981294c2e9ed145d38077d438', '\xabb84847a3d247a0ae7d8ce04063afc7', '2014-08-02 14:01:19.545-04');

/* testVideoComponentB -> PartB -> project A -> course A -> user A (teacher)*/
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('\x50d07485f33c47559ccf59d823cbb79e', '\xabb84847a3d247a0ae7d8ce04063afc7', '2014-08-03 14:01:19.545-04');


/* ---------------------- SCHEDULES ---------------------- */

/* CourseSchedule A -> Course A */
INSERT INTO course_schedules (id, course_id, version, day, start_time, end_time, description, created_at, updated_at)
VALUES ('\x308792b27a2943c8ad51a5c4f306cdaf', '\x217c5622ff9e43728e6a95fb3bae300b', 1, '2015-01-15', '2014-08-15 14:01:19.545-04', '2014-08-15 15:01:19.545-04', 'test CourseSchedule A description', '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* CourseSchedule B -> Course B */
INSERT INTO course_schedules (id, course_id, version, day, start_time, end_time, description, created_at, updated_at)
VALUES ('\xdc1190c2b5fd4bac95fa7d67e1f1d445', '\x404c800a53854e6b867e365a1e6b00de', 2, '2015-01-16', '2014-08-16 16:01:19.545-04', '2014-08-16 17:01:19.545-04', 'test CourseSchedule B description', '2014-08-04 14:01:19.545-04','2014-08-05 14:01:19.545-04');

/* CourseSchedule C -> Course B */
INSERT INTO course_schedules (id, course_id, version, day, start_time, end_time, description, created_at, updated_at)
VALUES ('\x6df9d164b1514c389acd6b91301a199d', '\x404c800a53854e6b867e365a1e6b00de', 3, '2015-01-17', '2014-08-17 18:01:19.545-04', '2014-08-17 19:01:19.545-04', 'test CourseSchedule C description', '2014-08-06 14:01:19.545-04','2014-08-07 14:01:19.545-04');


/* ---------------------- SCHEDULE_EXCEPTIONS ---------------------- */

/* SectionScheduleException A -> UserC -> CourseA */
INSERT INTO course_schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\xda17e24aa5454d7494e1427896e13ebe', '\xf5f984073a0b4ea5952a575886e90586', '\x217c5622ff9e43728e6a95fb3bae300b', 1, '2014-08-01', '2014-08-01 14:01:19.545-04', '2014-08-01 15:01:19.545-04', 'testCourseScheduleExceptionA reason', '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* SectionScheduleException B -> UserC -> CourseB */
INSERT INTO course_schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\x3a285f0c66d041b2851bcfcd203550d9', '\xf5f984073a0b4ea5952a575886e90586', '\x404c800a53854e6b867e365a1e6b00de', 2, '2014-08-02', '2014-08-02 16:01:19.545-04', '2014-08-02 17:01:19.545-04', 'testCourseScheduleExceptionB reason','2014-08-04 14:01:19.545-04','2014-08-05 14:01:19.545-04');

/* SectionScheduleException C -> UserE -> CourseB */
INSERT INTO course_schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\x4d7ca313f2164f5985ae88bcbca70317', '\x871b525067124e548ab60784cae0bc64', '\x404c800a53854e6b867e365a1e6b00de', 3, '2014-08-03', '2014-08-03 18:01:19.545-04', '2014-08-03 19:01:19.545-04', 'testCourseScheduleExceptionC reason','2014-08-06 14:01:19.545-04','2014-08-07 14:01:19.545-04');

/* SectionScheduleException D -> UserE -> CourseB */
INSERT INTO course_schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\xb9a1cd293c04450e9b4a2a63a6871c35', '\x871b525067124e548ab60784cae0bc64', '\x404c800a53854e6b867e365a1e6b00de', 4, '2014-08-04', '2014-08-04 20:01:19.545-04', '2014-08-04 21:01:19.545-04', 'testCourseScheduleExceptionD reason','2014-08-08 14:01:19.545-04','2014-08-09 14:01:19.545-04');


/* ---------------------- JOURNAL ---------------------- */

/* JournalEntryA -> userA -> projectA */
INSERT INTO journal_201401 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('\x6aabd410735f4023ae049f67f84a3846', 1, '\x36c8c0ca50aa4806afa5916a5e33a81f', '\xc9b4cfceaed448fd94f5c980763dfddc', 'view', 'item 1', '2014-01-01 14:01:19.545-04','2014-01-02 14:01:19.545-04');

/* JournalEntryB -> userA -> projectA */
INSERT INTO journal_201402 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('\x2ec1c7979a604b11a860259ae0f59134', 2, '\x36c8c0ca50aa4806afa5916a5e33a81f', '\xc9b4cfceaed448fd94f5c980763dfddc', 'click', 'item 2', '2014-02-03 14:01:19.545-04','2014-02-04 14:01:19.545-04');

/* JournalEntryC -> userA -> projectA */
INSERT INTO journal_201403 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('\xf7a218444c764711ae135e8a3758cefb', 3, '\x36c8c0ca50aa4806afa5916a5e33a81f', '\xc9b4cfceaed448fd94f5c980763dfddc', 'watch', 'item 3', '2014-03-05 14:01:19.545-04','2014-03-06 14:01:19.545-04');

/* JournalEntryD -> userA -> projectA */
INSERT INTO journal_201404 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('\xd77a1706e2304798853f257cad2ed627', 4, '\x36c8c0ca50aa4806afa5916a5e33a81f', '\xc9b4cfceaed448fd94f5c980763dfddc', 'listen', 'item 4', '2014-04-07 14:01:19.545-04','2014-04-08 14:01:19.545-04');

/* JournalEntryE -> userB -> projectB */
INSERT INTO journal_201405 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('\x0d809bb8779b4e55817cf995959ff290', 5, '\x6c0e29bdd05b4b2981156be93e936c59', '\xe4ae3b9098714339b05c8d39e3aaf65d', 'write', 'item 5', '2014-05-09 14:01:19.545-04','2014-05-10 14:01:19.545-04');

/* JournalEntryF -> userB -> projectB */
INSERT INTO journal_201406 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('\x594021024fb14fada0e1605b37e09965', 6, '\x6c0e29bdd05b4b2981156be93e936c59', '\xe4ae3b9098714339b05c8d39e3aaf65d', 'create', 'item 6', '2014-06-11 14:01:19.545-04','2014-06-12 14:01:19.545-04');

/* JournalEntryG -> userB -> projectB */
INSERT INTO journal_201407 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('\xeab8e5d1d88c4718b1bfda524daca133', 7, '\x6c0e29bdd05b4b2981156be93e936c59', '\xe4ae3b9098714339b05c8d39e3aaf65d', 'update', 'item 7', '2014-07-13 14:01:19.545-04','2014-07-14 14:01:19.545-04');

/* JournalEntryH -> userB -> projectB */
INSERT INTO journal_201408 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('\x453c7d392bbd40419fb46fb59a134395', 8, '\x6c0e29bdd05b4b2981156be93e936c59', '\xe4ae3b9098714339b05c8d39e3aaf65d', 'delete', 'item 8', '2014-08-15 14:01:19.545-04','2014-08-16 14:01:19.545-04');

/* JournalEntryI -> userA -> projectA */
INSERT INTO journal_201409 (id, version, user_id, project_id, entry_type, item, created_at, updated_at)
VALUES ('\xcc19d1cd9114413a96ba46c981525e30', 9, '\x36c8c0ca50aa4806afa5916a5e33a81f', '\xc9b4cfceaed448fd94f5c980763dfddc', 'delete', 'item 9', '2014-09-17 14:01:19.545-04','2014-09-18 14:01:19.545-04');


/* ---------------------- CHAT_LOGS ---------------------- */

/* chatA */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 1, '\x871b525067124e548ab60784cae0bc64', 'testChatA message', false, '2014-08-01 14:01:19.545-04');

/* chatB */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 2, '\xf5f984073a0b4ea5952a575886e90586', 'testChatB message', false, '2014-08-02 14:01:19.545-04');

/* chatC */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 3, '\xf5f984073a0b4ea5952a575886e90586', 'testChatC message', false, '2014-08-03 14:01:19.545-04');

/* chatD */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 4, '\x871b525067124e548ab60784cae0bc64', 'testChatD message', false, '2014-08-04 14:01:19.545-04');

/* chatE */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 5, '\xf5f984073a0b4ea5952a575886e90586', 'testChatE message', false, '2014-08-05 14:01:19.545-04');

/* chatF */
INSERT INTO chat_logs (course_id, message_num, user_id, message, hidden, created_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 6, '\xf5f984073a0b4ea5952a575886e90586', 'testChatF message', false, '2014-08-06 14:01:19.545-04');
