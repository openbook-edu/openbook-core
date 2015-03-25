/* USERS */
/* user A */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', 1, 'testUserA@example.com', 'testUserA', '$s0$100801$SIZ9lgHz0kPMgtLB37Uyhw==$wyKhNrg/MmUvlYuVygDctBE5LHBjLB91nyaiTpjbeyM=', 'TestA', 'UserA', '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

/* user B */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', 2, 'testUserB@example.com', 'testUserB', '$s0$100801$84r2edPRqM/8xFCe+G1PPw==$p7dTGjBJpGUMoyQ1Nqat1i4SBV6aT6BX7h1WU6cLRnc=', 'TestB', 'UserB', '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

/* user C no references in other tables */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', 3, 'testUserC@example.com', 'testUserC', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestC', 'UserC', '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

/* user E (student) */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x871b525067124e548ab60784cae0bc64', 4, 'testUserE@example.com', 'testUserE', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestE', 'UserE', '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

/* user F */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x4d01347ec5924e5fb09fdd281b3d9b87', 5, 'testUserF@example.com', 'testUserF', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestF', 'UserF', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* user G */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\xc4d948967e1b45fabae74fb3a89a4d63', 6, 'testUserG@example.com', 'testUserG', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestG', 'UserG', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* user H */
INSERT INTO users (id, version, email, username, password_hash, givenname, surname, created_at, updated_at)
VALUES ('\x5099a6b48809400d8e380119184d0f93', 7, 'testUserH@example.com', 'testUserH', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestH', 'UserH', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');


/* ROLES */
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


/* USERS_ROLES*/
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


/* COURSES */
/* course A -> user A (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 1, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'test course A', 1574408, '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* course B -> user B (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x404c800a53854e6b867e365a1e6b00de', 2, '\x6c0e29bdd05b4b2981156be93e936c59', 'test course B', 2230288, '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* course D -> user F (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x94cc65bb45424f628e08d58522e7b5f1', 3, '\x4d01347ec5924e5fb09fdd281b3d9b87', 'test course D', 269368, '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* course F -> user F (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x287b61f5da6b4de785353bc500cffac7', 4, '\x4d01347ec5924e5fb09fdd281b3d9b87', 'test course F', 269368, '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');

/* course G -> user F (teacher) */
INSERT INTO courses (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\xb24abba8e6c74700900ce66ed0185a70', 5, '\x4d01347ec5924e5fb09fdd281b3d9b87', 'test course G', 1508909, '2014-08-17 14:01:19.545-04', '2014-08-18 14:01:19.545-04');


/* USERS_COURSES */
/* UserC (student) -> CourseA -> user A (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\x217c5622ff9e43728e6a95fb3bae300b', '2014-08-05 14:01:19.545-04');

/* UserC (student) -> CourseB -> user B (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', '\x404c800a53854e6b867e365a1e6b00de', '2014-08-06 14:01:19.545-04');

/* UserE (student) -> CourseB -> user B (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\x871b525067124e548ab60784cae0bc64', '\x404c800a53854e6b867e365a1e6b00de', '2014-08-07 14:01:19.545-04');

/* UserG (student) -> CourseF -> user F (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\xc4d948967e1b45fabae74fb3a89a4d63', '\x287b61f5da6b4de785353bc500cffac7', '2014-08-08 14:01:19.545-04');

/* UserH (student) -> CourseF -> user F (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\x5099a6b48809400d8e380119184d0f93', '\x287b61f5da6b4de785353bc500cffac7', '2014-08-10 14:01:19.545-04');

/* UserH (student) -> CourseD -> user F (teacher) */
INSERT INTO users_courses (user_id, course_id, created_at)
VALUES ('\x5099a6b48809400d8e380119184d0f93', '\x94cc65bb45424f628e08d58522e7b5f1', '2014-08-10 14:01:19.545-04');


/* PROJECTS */
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


/* PARTS */
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


/* TASKS */
/* longAnswerTask A -> part A -> project A -> course A -> user A (teacher)*/
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, created_at, updated_at)
VALUES ('\xbf1a6ed09f834cb485c1ad456299b3a3', 1, '\x5cd214be6bba47fa9f350eb8bafec397', null, 'test longAnswerTask A', 'test longAnswerTask A description', 10, 0, true, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

INSERT INTO long_answer_tasks (task_id)
VALUES ('\xbf1a6ed09f834cb485c1ad456299b3a3');

/* shortAnswerTask B -> part A (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, created_at, updated_at)
VALUES ('\x10ef05ee7b494352b86e70510adf617f', 2, '\x5cd214be6bba47fa9f350eb8bafec397', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test shortAnswerTask B', 'test shortAnswerTask B description', 11, 1, true, '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

INSERT INTO short_answer_tasks (task_id, max_length)
VALUES ('\x10ef05ee7b494352b86e70510adf617f', 51);

/* multipleChoiceTask C -> part A (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, created_at, updated_at)
VALUES ('\x76cc2ed7611b4dafaa3f20efe42a65a0', 3, '\x5cd214be6bba47fa9f350eb8bafec397', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test MultipleChoiceTask C', 'test MultipleChoiceTask C description', 12, 2, true, '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

INSERT INTO multiple_choice_tasks (task_id, choices, answers, allow_multiple, randomize)
VALUES ('\x76cc2ed7611b4dafaa3f20efe42a65a0', '{choice 1, choice 2}', '{1, 2}', false, true);

/* orderingTask D -> part B (dependency_id -> longAnswerTask A) -> project A -> course A -> user A (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, created_at, updated_at)
VALUES ('\x808400838923476fa8738ba6c55e30c8', 4, '\xabb84847a3d247a0ae7d8ce04063afc7', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test OrderingTask D', 'test OrderingTask D description', 13, 3, true, '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04');

INSERT INTO ordering_tasks (task_id, elements, answers, randomize)
VALUES ('\x808400838923476fa8738ba6c55e30c8', '{element 3, element 4}', '{3, 4}', true);

/* matchingTask E -> part C (dependency_id -> longAnswerTask A) -> project B -> course B -> user B (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, created_at, updated_at)
VALUES ('\x7e9fe0e8e8214d84a7feac023fe6dfa3', 5, '\xfb01f11b7f2341c8877b68410be62aa5', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test MatchingTask E', 'test MatchingTask E description', 14, 4, true, '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

INSERT INTO matching_tasks (task_id, elements_left, elements_right, answers, randomize)
VALUES ('\x7e9fe0e8e8214d84a7feac023fe6dfa3', '{choice left 5, choice left 6}', '{choice right 7, choice right 8}', '{{5, 6}, {7, 8}}', true);

/* matchingTask K -> partE (dependency_id -> longAnswerTask A) -> project C -> course B -> user B (teacher) */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, created_at, updated_at)
VALUES ('\x337fa73136854ba38668280c0096514c', 6, '\xc850ec53f0a9460d918a5e6fd538f376', '\xbf1a6ed09f834cb485c1ad456299b3a3', 'test MatchingTask K', 'test MatchingTask K description', 16, 4, true, '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

INSERT INTO matching_tasks (task_id, elements_left, elements_right, answers, randomize)
VALUES ('\x337fa73136854ba38668280c0096514c', '{choice left 6, choice left 7}', '{choice right 8, choice right 9}', '{{6, 7}, {8, 9}}', true);


/* DOCUMENTS */
/* documentA -> userC*/
INSERT INTO documents (id, version, owner_id, title, plaintext, delta, created_at, updated_at)
VALUES ('\xfd923b3f6dc2472e8ce77a8fcc6a1a20', 1, '\xf5f984073a0b4ea5952a575886e90586', 'testDocumentA title', 'testDocumentA plaintext', null, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');


/* WORK */
/* longAnswerWorkA -> userC -> longAnswerTaskA -> documentA */
INSERT INTO work (id, user_id, task_id, version, is_complete, work_type, created_at, updated_at)
VALUES ('\x441374e20b1643ecadb96a3251081d24', '\xf5f984073a0b4ea5952a575886e90586', '\xbf1a6ed09f834cb485c1ad456299b3a3', 1, true, 0, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

INSERT INTO long_answer_work (work_id, document_id)
VALUES ('\x441374e20b1643ecadb96a3251081d24', '\xfd923b3f6dc2472e8ce77a8fcc6a1a20');


/* COMPONENTS */
/* testTextComponentA */
INSERT INTO components (id, version, title, questions, things_to_think_about, type, created_at, updated_at)
VALUES ('\x8cfc608981294c2e9ed145d38077d438', 1, 'testTextComponentA title', 'testTextComponentA questions', 'testTextComponentA thingsToThinkAbout', 'text', '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

INSERT INTO text_components (component_id, content)
VALUES ('\x8cfc608981294c2e9ed145d38077d438', 'testTextComponentA content');

/* testVideoComponentB */
INSERT INTO components (id, version, title, questions, things_to_think_about, type, created_at, updated_at)
VALUES ('\x50d07485f33c47559ccf59d823cbb79e', 2, 'testVideoComponentB title', 'testVideoComponentB questions', 'testVideoComponentB thingsToThinkAbout', 'video', '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

INSERT INTO video_components (component_id, vimeo_id, width, height)
VALUES ('\x50d07485f33c47559ccf59d823cbb79e', '19579282', 640, 480);

/* testAudionComponentC */
INSERT INTO components (id, version, title, questions, things_to_think_about, type, created_at, updated_at)
VALUES ('\xa51c6b535180416daa771cc620dee9c0', 3, 'testAudioComponentC title', 'testAudioComponentC questions', 'testAudioComponentC thingsToThinkAbout', 'audio', '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');

INSERT INTO audio_components (component_id, soundcloud_id)
VALUES ('\xa51c6b535180416daa771cc620dee9c0', 'dj-whisky-ft-nozipho-just');


/* PARTS_COMPONENTS*/
/* testTextComponentA -> PartA */
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('\x8cfc608981294c2e9ed145d38077d438', '\x5cd214be6bba47fa9f350eb8bafec397', '2014-08-01 14:01:19.545-04');

/* testTextComponentA -> PartB */
INSERT INTO parts_components (component_id, part_id, created_at)
VALUES ('\x8cfc608981294c2e9ed145d38077d438', '\xabb84847a3d247a0ae7d8ce04063afc7', '2014-08-02 14:01:19.545-04');

/* SCHEDULES */
/* CourseSchedule A -> Course A */
INSERT INTO schedules (id, course_id, version, start_time, length, reason, created_at, updated_at)
VALUES ('\x308792b27a2943c8ad51a5c4f306cdaf', '\x217c5622ff9e43728e6a95fb3bae300b', 1, '2015-01-15 14:38:19-04', 12345, 'test CourseSchedule A reason', '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* CourseSchedule B -> Course B */
INSERT INTO schedules (id, course_id, version, start_time, length, reason, created_at, updated_at)
VALUES ('\xdc1190c2b5fd4bac95fa7d67e1f1d445', '\x404c800a53854e6b867e365a1e6b00de', 2, '2015-01-16 12:38:19-04', 12345, 'test CourseSchedule B reason', '2014-08-04 14:01:19.545-04','2014-08-05 14:01:19.545-04');

/* CourseSchedule C -> Course B */
INSERT INTO schedules (id, course_id, version, start_time, length, reason, created_at, updated_at)
VALUES ('\x6df9d164b1514c389acd6b91301a199d', '\x404c800a53854e6b867e365a1e6b00de', 3, '2015-01-17 16:38:19-04', 12345, 'test CourseSchedule C reason', '2014-08-06 14:01:19.545-04','2014-08-07 14:01:19.545-04');


/* SCHEDULE_EXCEPTIONS */
/* SectionScheduleException A -> UserA -> CourseA */
INSERT INTO schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\xda17e24aa5454d7494e1427896e13ebe', '\x36c8c0ca50aa4806afa5916a5e33a81f', '\x217c5622ff9e43728e6a95fb3bae300b', 1, '2014-08-01', '2014-08-01 14:01:19.545-04', '2014-08-01 15:01:19.545-04', 'testSectionScheduleExceptionA reason', '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* SectionScheduleException B -> UserB -> CourseB */
INSERT INTO schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\x3a285f0c66d041b2851bcfcd203550d9', '\x6c0e29bdd05b4b2981156be93e936c59', '\x404c800a53854e6b867e365a1e6b00de', 2, '2014-08-04', '2014-08-04 16:01:19.545-04', '2014-08-04 17:01:19.545-04', 'testSectionScheduleExceptionB reason','2014-08-05 14:01:19.545-04','2014-08-06 14:01:19.545-04');

/* SectionScheduleException C -> UserE -> CourseB */
INSERT INTO schedule_exceptions (id, user_id, course_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\x4d7ca313f2164f5985ae88bcbca70317', '\x871b525067124e548ab60784cae0bc64', '\x404c800a53854e6b867e365a1e6b00de', 3, '2014-08-07', '2014-08-07 10:01:19.545-04', '2014-08-07 11:01:19.545-04', 'testSectionScheduleExceptionC reason','2014-08-08 14:01:19.545-04','2014-08-09 14:01:19.545-04');
