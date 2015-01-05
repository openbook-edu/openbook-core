/* USERS */
/* user A*/
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', 1, '2014-08-01 14:01:19.545-04', '2014-08-02 14:01:19.545-04', 'testUserA', 'testUserA@example.com', '$s0$100801$SIZ9lgHz0kPMgtLB37Uyhw==$wyKhNrg/MmUvlYuVygDctBE5LHBjLB91nyaiTpjbeyM=', 'TestA', 'UserA');

/* user B*/
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', 2, '2014-08-03 14:01:19.545-04', '2014-08-04 14:01:19.545-04', 'testUserB', 'testUserB@example.com', '$s0$100801$84r2edPRqM/8xFCe+G1PPw==$p7dTGjBJpGUMoyQ1Nqat1i4SBV6aT6BX7h1WU6cLRnc=', 'TestB', 'UserB');

/* user C */
INSERT INTO users (id, version, created_at, updated_at, username, email, password_hash, givenname, surname)
VALUES ('\xf5f984073a0b4ea5952a575886e90586', 3, '2014-08-05 14:01:19.545-04', '2014-08-06 14:01:19.545-04', 'testUserC', 'testUserC@example.com', '$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo=', 'TestC', 'UserC');


/* CLASSES */
/* class A */
INSERT INTO classes (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x217c5622ff9e43728e6a95fb3bae300b', 1, '\x36c8c0ca50aa4806afa5916a5e33a81f', 'test class A', 1574408, '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04');

/* class B*/
INSERT INTO classes (id, version, teacher_id, name, color, created_at, updated_at)
VALUES ('\x404c800a53854e6b867e365a1e6b00de', 2, '\x6c0e29bdd05b4b2981156be93e936c59', 'test class B', 2230288, '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04');


/* USERS_CLASSES */
/* UserA -> ClassA*/
INSERT INTO users_classes (user_id, class_id, created_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', '\x217c5622ff9e43728e6a95fb3bae300b', '2014-08-07 14:01:19.545-04');

/* UserB -> ClassB*/
INSERT INTO users_classes (user_id, class_id, created_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\x404c800a53854e6b867e365a1e6b00de', '2014-08-08 14:01:19.545-04');


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


/* USERS_ROLES*/
/* UserA -> RoleA */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', '\x1430e95077f94b30baf8bb226fc7091a', '2014-08-13 14:01:19.545-04');

/* UserA -> RoleB */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x36c8c0ca50aa4806afa5916a5e33a81f', '\xa011504cd11840cdb9eb6e10d5738c67', '2014-08-14 14:01:19.545-04');

/* UserB -> RoleB */
INSERT INTO users_roles (user_id, role_id, created_at)
VALUES ('\x6c0e29bdd05b4b2981156be93e936c59', '\xa011504cd11840cdb9eb6e10d5738c67', '2014-08-15 14:01:19.545-04');


/* PROJECTS */
/* project A -> class A */
INSERT INTO projects (id, version, class_id, name, slug, description, availability, created_at, updated_at)
VALUES ('\xc9b4cfceaed448fd94f5c980763dfddc', 1, '\x217c5622ff9e43728e6a95fb3bae300b', 'test project A', 'test project slug A', 'test project A description', 'any', '2014-08-09 14:01:19.545-04', '2014-08-10 14:01:19.545-04')

/* project B -> class B */
INSERT INTO projects (id, version, class_id, name, slug, description, availability, created_at, updated_at)
VALUES ('\xe4ae3b9098714339b05c8d39e3aaf65d', 2, '\x404c800a53854e6b867e365a1e6b00de', 'test project B', 'test project slug B', 'test project B description', 'free', '2014-08-11 14:01:19.545-04', '2014-08-12 14:01:19.545-04')

/* project C -> class B */
INSERT INTO projects (id, version, class_id, name, slug, description, availability, created_at, updated_at)
VALUES ('\x4ac4d872451b4092b13f643d6d5fa930', 3, '\x404c800a53854e6b867e365a1e6b00de', 'test project C', 'test project slug C', 'test project C description', 'class', '2014-08-13 14:01:19.545-04', '2014-08-14 14:01:19.545-04')

