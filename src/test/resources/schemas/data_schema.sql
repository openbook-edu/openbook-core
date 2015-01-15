/* USERS */
/* user A */
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', 1, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04', 'testUserA', 'testUserA@example.com', '$s0$100801$SIZ9lgHz0kPMgtLB37Uyhw==$wyKhNrg/MmUvlYuVygDctBE5LHBjLB91nyaiTpjbeyM=', 'TestA', 'UserA');

/* user B */
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', 2, '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04', 'testUserB', 'testUserB@example.com', '$s0$100801$84r2edPRqM/8xFCe+G1PPw==$p7dTGjBJpGUMoyQ1Nqat1i4SBV6aT6BX7h1WU6cLRnc=', 'TestB', 'UserB');

/* user C no references in other tables */
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', 3, '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04', 'testUserC', 'testUserC@example.com', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestC', 'UserC');

/* user E (student) */
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\x871b525067124e548ab60784cae0bc64', 4, '2014-08-07 14:01:19.545-04', '2014-08-08 14:01:19.545-04', 'testUserE', 'testUserE@example.com', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestE', 'UserE');

/* user F */
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\x4d01347ec5924e5fb09fdd281b3d9b87', 5, '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04', 'testUserF', 'testUserF@example.com', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestF', 'UserF');

/* user G */
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\xc4d948967e1b45fabae74fb3a89a4d63', 6, '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04', 'testUserG', 'testUserG@example.com', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestG', 'UserG');

/* user H */
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\x5099a6b48809400d8e380119184d0f93', 7, '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04', 'testUserH', 'testUserH@example.com', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestH', 'UserH');


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


/* CLASS_SCHEDULES */
/* ClassSchedule A -> Class A */
INSERT INTO class_schedules (id, version, class_id, day, start_time, end_time, description, created_at, updated_at)
VALUES ('\x308792b27a2943c8ad51a5c4f306cdaf', 1, '\x217c5622ff9e43728e6a95fb3bae300b', '2015-01-15', '2015-01-15 14:38:19-04', '2015-01-15 15:38:19-04', 'test ClassSchedule A description', '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* ClassSchedule B -> Class B */
INSERT INTO class_schedules (id, version, class_id, day, start_time, end_time, description, created_at, updated_at)
VALUES ('\xdc1190c2b5fd4bac95fa7d67e1f1d445', 2, '\x404c800a53854e6b867e365a1e6b00de', '2015-01-16', '2015-01-16 12:38:19-04', '2015-01-16 13:38:19-04', 'test ClassSchedule B description', '2014-08-04 14:01:19.545-04','2014-08-05 14:01:19.545-04');

/* ClassSchedule C -> Class B */
INSERT INTO class_schedules (id, version, class_id, day, start_time, end_time, description, created_at, updated_at)
VALUES ('\x6df9d164b1514c389acd6b91301a199d', 3, '\x404c800a53854e6b867e365a1e6b00de', '2015-01-17', '2015-01-17 16:38:19-04', '2015-01-17 17:38:19-04', 'test ClassSchedule C description', '2014-08-06 14:01:19.545-04','2014-08-07 14:01:19.545-04');


/* CLASS_SCHEDULE_EXCEPTIONS */
/* SectionScheduleException A -> UserA -> ClassA */
INSERT INTO class_schedule_exceptions (id, user_id, class_id, version, day, start_time, end_time, created_at, updated_at)
VALUES ('\xda17e24aa5454d7494e1427896e13ebe', '\x36c8c0ca50aa4806afa5916a5e33a81f', '\x217c5622ff9e43728e6a95fb3bae300b', 1, '2014-08-01', '2014-08-01 14:01:19.545-04', '2014-08-01 15:01:19.545-04', '2014-08-02 14:01:19.545-04','2014-08-03 14:01:19.545-04');

/* SectionScheduleException B -> UserB -> ClassB */
INSERT INTO class_schedule_exceptions (id, user_id, class_id, version, day, start_time, end_time, created_at, updated_at)
VALUES ('\x3a285f0c66d041b2851bcfcd203550d9', '\x6c0e29bdd05b4b2981156be93e936c59', '\x404c800a53854e6b867e365a1e6b00de', 2, '2014-08-04', '2014-08-04 16:01:19.545-04', '2014-08-04 17:01:19.545-04', '2014-08-05 14:01:19.545-04','2014-08-06 14:01:19.545-04');

/* SectionScheduleException C -> UserE -> ClassB */
INSERT INTO class_schedule_exceptions (id, user_id, class_id, version, day, start_time, end_time, created_at, updated_at)
VALUES ('\x4d7ca313f2164f5985ae88bcbca70317', '\x871b525067124e548ab60784cae0bc64', '\x404c800a53854e6b867e365a1e6b00de', 3, '2014-08-07', '2014-08-07 10:01:19.545-04', '2014-08-07 11:01:19.545-04', '2014-08-08 14:01:19.545-04','2014-08-09 14:01:19.545-04');


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
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\xa011504cd11840cdb9eb6e10d5738c67', '2014-08-15 14:01:19.545-04');

/* UserB -> RoleF */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\x45b3552707ad4c4f9051f0e755216163', '2014-08-15 14:01:19.545-04');

/* UserB -> RoleG */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\x45cc7cc8987645f58efeccd4dba7ea69', '2014-08-15 14:01:19.545-04');


/* PROJECTS */
/* project A -> class A */
INSERT INTO projects (id, version, class_id, name, slug, description, availability, created_at, updated_at)
VALUES ('\xc9b4cfceaed448fd94f5c980763dfddc', 1, '\x217c5622ff9e43728e6a95fb3bae300b', 'test project A', 'test project slug A', 'test project A description', 'any', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* project B -> class B */
INSERT INTO projects (id, version, class_id, name, slug, description, availability, created_at, updated_at)
VALUES ('\xe4ae3b9098714339b05c8d39e3aaf65d', 2, '\x404c800a53854e6b867e365a1e6b00de', 'test project B', 'test project slug B', 'test project B description', 'free', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');

/* project C -> class B */
INSERT INTO projects (id, version, class_id, name, slug, description, availability, created_at, updated_at)
VALUES ('\x4ac4d872451b4092b13f643d6d5fa930', 3, '\x404c800a53854e6b867e365a1e6b00de', 'test project C', 'test project slug C', 'test project C description', 'class', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04');

/* CLASSES_PROJECTS - table should be deleted */

/* PARTS */
/* part A -> project A */
INSERT INTO parts (id, version, project_id, name, description, position, enabled, created_at, updated_at)
VALUES ('\x5cd214be6bba47fa9f350eb8bafec397', 1, '\xc9b4cfceaed448fd94f5c980763dfddc', 'test part A', 'test part A description', 10, true,'2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04');

/* part B -> project A */
INSERT INTO parts (id, version, project_id, name, description, position, enabled, created_at, updated_at)
VALUES ('\xabb84847a3d247a0ae7d8ce04063afc7', 2, '\xc9b4cfceaed448fd94f5c980763dfddc', 'test part B', 'test part B description', 11, true,'2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04');

/* part C -> project B */
INSERT INTO parts (id, version, project_id, name, description, position, enabled, created_at, updated_at)
VALUES ('\xfb01f11b7f2341c8877b68410be62aa5', 3, '\xe4ae3b9098714339b05c8d39e3aaf65d', 'test part C', 'test part C description', 12, true,'2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04');


/* SCHEDULED_CLASSES_PARTS - table should be deleted */

