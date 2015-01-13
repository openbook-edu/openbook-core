import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.uuid.UUID
import org.joda.time.{DateTimeZone, DateTime, LocalDate, LocalTime}
import java.awt.Color

object TestValues {

  /* USERS */
  val testUserA = User(
    id = UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"),
    version = 1L,
    email = "testUserA@example.com",
    username = "testUserA",
    passwordHash = Some("$s0$100801$SIZ9lgHz0kPMgtLB37Uyhw==$wyKhNrg/MmUvlYuVygDctBE5LHBjLB91nyaiTpjbeyM="),
    givenname = "TestA",
    surname = "UserA",
    createdAt = Option(new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testUserB = User(
    id = UUID("6c0e29bd-d05b-4b29-8115-6be93e936c59"),
    version = 2L,
    email = "testUserB@example.com",
    username = "testUserB",
    passwordHash = Some("$s0$100801$84r2edPRqM/8xFCe+G1PPw==$p7dTGjBJpGUMoyQ1Nqat1i4SBV6aT6BX7h1WU6cLRnc="),
    givenname = "TestB",
    surname = "UserB",
    createdAt = Option(new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  /**
   * User has no references in other tables
   */
  val testUserC = User(
    id = UUID("f5f98407-3a0b-4ea5-952a-575886e90586"),
    version = 3L,
    email = "testUserC@example.com",
    username = "testUserC",
    passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestC",
    surname = "UserC",
    createdAt = Option(new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  /**
   * No data in DB
   */
  val testUserD = User(
    id = UUID("4d97f26c-df3f-4866-8919-11f51f14e9c4"),
    email = "testUserD@example.com",
    username = "testUserD",
    passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3W1234="),
    givenname = "TestD",
    surname = "UserD"
  )

  val testUserE = User(
    id = UUID("871b5250-6712-4e54-8ab6-0784cae0bc64"),
    version = 4L,
    email = "testUserE@example.com",
    username = "testUserE",
    passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestE",
    surname = "UserE",
    createdAt = Option(new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testUserF = User(
    id = UUID("4d01347e-c592-4e5f-b09f-dd281b3d9b87"),
    version = 5L,
    email = "testUserF@example.com",
    username = "testUserF",
    passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestF",
    surname = "UserF",
    createdAt = Option(new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testUserG = User(
    id = UUID("c4d94896-7e1b-45fa-bae7-4fb3a89a4d63"),
    version = 6L,
    email = "testUserG@example.com",
    username = "testUserG",
    passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestG",
    surname = "UserG",
    createdAt = Option(new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testUserH = User(
    id = UUID("5099a6b4-8809-400d-8e38-0119184d0f93"),
    version = 7L,
    email = "testUserH@example.com",
    username = "testUserH",
    passwordHash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestH",
    surname = "UserH",
    createdAt = Option(new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )


  /* CLASSES */
  val testClassA = Class(
    id = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"),
    version = 1L,
    teacherId = Option(testUserA.id),
    name = "test class A",
    color = new Color(24, 6, 8),
    createdAt = Option(new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testClassB = Class(
    id = UUID("404c800a-5385-4e6b-867e-365a1e6b00de"),
    version = 2L,
    teacherId = Option(testUserB.id),
    name = "test class B",
    color = new Color(34, 8, 16),
    createdAt = Option(new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  /**
   * No data in DB
   */
  val testClassC = Class(
    id = UUID("7cf524fa-aa7f-4bfe-93d7-8cd7787fd030"),
    teacherId = Option(UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f")), // testUserA.id
    name = "unexisting class C",
    color = new Color(24, 6, 8)
  )

  val testClassD = Class(
    id = UUID("94cc65bb-4542-4f62-8e08-d58522e7b5f1"),
    version = 3L,
    teacherId = Option(testUserF.id),
    name = "test class D",
    color = new Color(4, 28, 56),
    createdAt = Option(new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  /**
   * No data in DB for insert
   */
  val testClassE = Class(
    id = UUID("d0b05b14-4a5f-4727-ac43-bd8671aab53c"),
    teacherId = Option(UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f")), // testUserA.id
    name = "unexisting class E",
    color = new Color(45, 10, 15)
  )


  /* SECTION SCHEDULE EXCEPTION */
  val testSectionScheduleExceptionA = SectionScheduleException(
    id = UUID("da17e24a-a545-4d74-94e1-427896e13ebe"),
    userId = UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"), // User A
    classId = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"), // Class A
    version = 1L,
    day = new LocalDate(2014, 8, 1),
    startTime = new LocalTime(14, 1, 19),
    endTime = new LocalTime(15, 1, 19),
    createdAt = Option(new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testSectionScheduleExceptionB = SectionScheduleException(
    id = UUID("3a285f0c-66d0-41b2-851b-cfcd203550d9"),
    userId = UUID("6c0e29bd-d05b-4b29-8115-6be93e936c59"), // User B
    classId = UUID("404c800a-5385-4e6b-867e-365a1e6b00de"), // Class B
    version = 2L,
    day = new LocalDate(2014, 8, 4),
    startTime = new LocalTime(16, 1, 19),
    endTime = new LocalTime(17, 1, 19),
    createdAt = Option(new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testSectionScheduleExceptionC = SectionScheduleException(
    id = UUID("4d7ca313-f216-4f59-85ae-88bcbca70317"),
    userId = UUID("871b5250-6712-4e54-8ab6-0784cae0bc64"), // User E
    classId = UUID("404c800a-5385-4e6b-867e-365a1e6b00de"), // Class B
    version = 3L,
    day = new LocalDate(2014, 8, 7),
    startTime = new LocalTime(10, 1, 19),
    endTime = new LocalTime(11, 1, 19),
    createdAt = Option(new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  /**
   * No data in DB
   */
  val testSectionScheduleExceptionD = SectionScheduleException(
    id = UUID("848c8b4f-f566-4d7f-8a16-b4a76107778a"),
    userId = UUID("871b5250-6712-4e54-8ab6-0784cae0bc64"), // User E
    classId = UUID("404c800a-5385-4e6b-867e-365a1e6b00de"), // Class B
    version = 4L,
    day = new LocalDate(2014, 8, 8),
    startTime = new LocalTime(12, 1, 19),
    endTime = new LocalTime(13, 1, 19),
    createdAt = Option(new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  /* ROLES */
  val testRoleA = Role(
    id = UUID("1430e950-77f9-4b30-baf8-bb226fc7091a"),
    version = 1L,
    name = "test role A",
    createdAt = Option(new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testRoleB = Role(
    id = UUID("a011504c-d118-40cd-b9eb-6e10d5738c67"),
    version = 2L,
    name = "test role B",
    createdAt = Option(new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testRoleC = Role(
    id = UUID("31a4c2e6-762a-4303-bbb8-e64c24048920"),
    version = 3L,
    name = "test role C",
    createdAt = Option(new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  /**
   * No data in DB
   */
  val testRoleD = Role(
    id = UUID("b82d356d-a1bb-4e07-b28f-d15060fb42c2"),
    name = "test role D"
  )

  /**
   * No data in DB
   */
  val testRoleE = Role(
    id = UUID("29a84d7b-f90a-4a26-a224-b70631fdfbe4"),
    name = "test role E"
  )

  val testRoleF = Role(
    id = UUID("45b35527-07ad-4c4f-9051-f0e755216163"),
    version = 4L,
    name = "test role F",
    createdAt = Option(new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testRoleG = Role(
    id = UUID("45cc7cc8-9876-45f5-8efe-ccd4dba7ea69"),
    version = 5L,
    name = "test role G",
    createdAt = Option(new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 18, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )


  /* PROJECTS */
  val testProjectA = Project(
    id = UUID("c9b4cfce-aed4-48fd-94f5-c980763dfddc"),
    classId = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"),
    version = 1L,
    name = "test project A",
    slug = "test project slug A",
    description = "test project A description",
    availability = "any",
    parts = Vector(),
    createdAt = Option(new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testProjectB = Project(
    id = UUID("e4ae3b90-9871-4339-b05c-8d39e3aaf65d"),
    classId = UUID("404c800a-5385-4e6b-867e-365a1e6b00de"),
    version = 2L,
    name = "test project B",
    slug = "test project slug B",
    description = "test project B description",
    availability = "free",
    parts = Vector(),
    createdAt = Option(new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  val testProjectC = Project(
    id = UUID("4ac4d872-451b-4092-b13f-643d6d5fa930"),
    classId = UUID("404c800a-5385-4e6b-867e-365a1e6b00de"),
    version = 3L,
    name = "test project C",
    slug = "test project slug C",
    description = "test project C description",
    availability = "class",
    parts = Vector(),
    createdAt = Option(new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )

  /**
   * No data in DB
   */
  val testProjectD = Project(
    id = UUID("00743ada-1d3a-4912-adc8-fb8a0b1b7443"),
    classId = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"),
    name = "test project D",
    slug = "test project slug D",
    description = "test project D description",
    availability = "class",
    parts = Vector()
  )
}