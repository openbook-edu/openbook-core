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

/* UserB -> RoleG */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\x45cc7cc8987645f58efeccd4dba7ea69', '2014-08-18 14:01:19.545-04');

/* UserF -> RoleC */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x4d01347ec5924e5fb09fdd281b3d9b87', '\x31a4c2e6762a4303bbb8e64c24048920', '2014-08-19 14:01:19.545-04');

/* UserF -> RoleH */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x4d01347ec5924e5fb09fdd281b3d9b87', '\x2a3edf38750a46aa84289fb08e648ee8', '2014-08-20 14:01:19.545-04');


/* CLASSES */
/* class A -> user A (teacher) */
INSERT INTO classes (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 1, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'test class A', 1574408, '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* class B -> user B (teacher) */
INSERT INTO classes (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x404c800a53854e6b867e365a1e6b00de', 2, '\x6c0e29bdd05b4b2981156be93e936c59', 'test class B', 2230288, '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* class D -> user F (teacher) */
INSERT INTO classes (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x94cc65bb45424f628e08d58522e7b5f1', 3, '\x4d01347ec5924e5fb09fdd281b3d9b87', 'test class D', 269368, '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* class F -> user F (teacher) */
INSERT INTO classes (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x287b61f5da6b4de785353bc500cffac7', 4, '\x4d01347ec5924e5fb09fdd281b3d9b87', 'test class F', 269368, '2014-08-15 14:01:19.545-04', '2014-08-16 14:01:19.545-04');


/* USERS_CLASSES */
/* UserA -> ClassA */
INSERT INTO users_classes (user_id, class_id, created_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', '\x217c5622ff9e43728e6a95fb3bae300b', '2014-08-07 14:01:19.545-04');

/* UserB -> ClassB */
INSERT INTO users_classes (user_id, class_id, created_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\x404c800a53854e6b867e365a1e6b00de', '2014-08-08 14:01:19.545-04');

/* UserE (student) -> ClassB */
INSERT INTO users_classes (user_id, class_id, created_at)
VALUES ('\x871b525067124e548ab60784cae0bc64', '\x404c800a53854e6b867e365a1e6b00de', '2014-08-09 14:01:19.545-04');

/* UserG -> ClassB */
INSERT INTO users_classes (user_id, class_id, created_at)
VALUES ('\xc4d948967e1b45fabae74fb3a89a4d63', '\x404c800a53854e6b867e365a1e6b00de', '2014-08-09 14:01:19.545-04');

/* UserH -> ClassB */
INSERT INTO users_classes (user_id, class_id, created_at)
VALUES ('\x5099a6b48809400d8e380119184d0f93', '\x404c800a53854e6b867e365a1e6b00de', '2014-08-10 14:01:19.545-04');


/* SCHEDULES */
/* ClassSchedule A -> Class A */
INSERT INTO schedules (id, class_id, version, start_time, length, reason, created_at, updated_at)
VALUES ('\x308792b27a2943c8ad51a5c4f306cdaf', '\x217c5622ff9e43728e6a95fb3bae300b', 1, '2015-01-15 14:38:19-04', 12345, 'test ClassSchedule A reason', '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* ClassSchedule B -> Class B */
INSERT INTO schedules (id, class_id, version, start_time, length, reason, created_at, updated_at)
VALUES ('\xdc1190c2b5fd4bac95fa7d67e1f1d445', '\x404c800a53854e6b867e365a1e6b00de', 2, '2015-01-16 12:38:19-04', 12345, 'test ClassSchedule B reason', '2014-08-04 14:01:19.545-04','2014-08-05 14:01:19.545-04');

/* ClassSchedule C -> Class B */
INSERT INTO schedules (id, class_id, version, start_time, length, reason, created_at, updated_at)
VALUES ('\x6df9d164b1514c389acd6b91301a199d', '\x404c800a53854e6b867e365a1e6b00de', 3, '2015-01-17 16:38:19-04', 12345, 'test ClassSchedule C reason', '2014-08-06 14:01:19.545-04','2014-08-07 14:01:19.545-04');


/* SCHEDULE_EXCEPTIONS */
/* SectionScheduleException A -> UserA -> ClassA */
INSERT INTO schedule_exceptions (id, user_id, class_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\xda17e24aa5454d7494e1427896e13ebe', '\x36c8c0ca50aa4806afa5916a5e33a81f', '\x217c5622ff9e43728e6a95fb3bae300b', 1, '2014-08-01', '2014-08-01 14:01:19.545-04', '2014-08-01 15:01:19.545-04', 'testSectionScheduleExceptionA reason', '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* SectionScheduleException B -> UserB -> ClassB */
INSERT INTO schedule_exceptions (id, user_id, class_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\x3a285f0c66d041b2851bcfcd203550d9', '\x6c0e29bdd05b4b2981156be93e936c59', '\x404c800a53854e6b867e365a1e6b00de', 2, '2014-08-04', '2014-08-04 16:01:19.545-04', '2014-08-04 17:01:19.545-04', 'testSectionScheduleExceptionB reason','2014-08-05 14:01:19.545-04','2014-08-06 14:01:19.545-04');

/* SectionScheduleException C -> UserE -> ClassB */
INSERT INTO schedule_exceptions (id, user_id, class_id, version, day, start_time, end_time, reason, created_at, updated_at)
VALUES ('\x4d7ca313f2164f5985ae88bcbca70317', '\x871b525067124e548ab60784cae0bc64', '\x404c800a53854e6b867e365a1e6b00de', 3, '2014-08-07', '2014-08-07 10:01:19.545-04', '2014-08-07 11:01:19.545-04', 'testSectionScheduleExceptionC reason','2014-08-08 14:01:19.545-04','2014-08-09 14:01:19.545-04');


/* PROJECTS */
/* project A -> class A */
INSERT INTO projects (id, class_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('\xc9b4cfceaed448fd94f5c980763dfddc', '\x217c5622ff9e43728e6a95fb3bae300b', 1, 'test project A', 'test project slug A', 'test project A description', 'any', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* project B -> class B */
INSERT INTO projects (id, class_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('\xe4ae3b9098714339b05c8d39e3aaf65d', '\x404c800a53854e6b867e365a1e6b00de', 2, 'test project B', 'test project slug B', 'test project B description', 'free', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* project C -> class B */
INSERT INTO projects (id, class_id, version, name, slug, description, availability, created_at, updated_at)
VALUES ('\x4ac4d872451b4092b13f643d6d5fa930', '\x404c800a53854e6b867e365a1e6b00de', 3, 'test project C', 'test project slug C', 'test project C description', 'class', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');


/* PARTS */
/* part A -> project A */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\x5cd214be6bba47fa9f350eb8bafec397', 1, '\xc9b4cfceaed448fd94f5c980763dfddc', 'test part A', true, 10, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

/* part B -> project A */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\xabb84847a3d247a0ae7d8ce04063afc7', 2, '\xc9b4cfceaed448fd94f5c980763dfddc', 'test part B', true, 11, '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

/* part C -> project B */
INSERT INTO parts (id, version, project_id, name, enabled, position, created_at, updated_at)
VALUES ('\xfb01f11b7f2341c8877b68410be62aa5', 3, '\xe4ae3b9098714339b05c8d39e3aaf65d', 'test part C', true, 12, '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');


/* TASKS */
INSERT INTO tasks (id, version, part_id, dependency_id, name, description, position, task_type, notes_allowed, created_at, updated_at)
VALUES ('\x5cd214be6bba47fa9f350eb8bafec397', 1, '\x5cd214be6bba47fa9f350eb8bafec397', null, 'name', 'desc', 10, 10, true, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

