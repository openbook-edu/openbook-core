import java.awt.Color

import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.models.work.LongAnswerWork
import ca.shiftfocus.uuid.UUID
import org.joda.time.{DateTime, DateTimeZone}
import ws.kahn.ot.{Operation, Delta}

object TestValues {
  /* USERS */
  val testUserA = User(
    id = UUID("36c8c0ca-50aa-4806-afa5-916a5e33a81f"),
    version = 1L,
    email = "testUserA@example.com",
    username = "testUserA",
    hash = Some("$s0$100801$SIZ9lgHz0kPMgtLB37Uyhw==$wyKhNrg/MmUvlYuVygDctBE5LHBjLB91nyaiTpjbeyM="),
    givenname = "TestA",
    surname = "UserA",
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserB = User(
    id = UUID("6c0e29bd-d05b-4b29-8115-6be93e936c59"),
    version = 2L,
    email = "testUserB@example.com",
    username = "testUserB",
    hash = Some("$s0$100801$84r2edPRqM/8xFCe+G1PPw==$p7dTGjBJpGUMoyQ1Nqat1i4SBV6aT6BX7h1WU6cLRnc="),
    givenname = "TestB",
    surname = "UserB",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * User has no references in other tables
   */
  val testUserC = User(
    id = UUID("f5f98407-3a0b-4ea5-952a-575886e90586"),
    version = 3L,
    email = "testUserC@example.com",
    username = "testUserC",
    hash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestC",
    surname = "UserC",
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB
   */
  val testUserD = User(
    id = UUID("4d97f26c-df3f-4866-8919-11f51f14e9c4"),
    email = "testUserD@example.com",
    username = "testUserD",
    hash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3W1234="),
    givenname = "TestD",
    surname = "UserD"
  )

  val testUserE = User(
    id = UUID("871b5250-6712-4e54-8ab6-0784cae0bc64"),
    version = 4L,
    email = "testUserE@example.com",
    username = "testUserE",
    hash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestE",
    surname = "UserE",
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserF = User(
    id = UUID("4d01347e-c592-4e5f-b09f-dd281b3d9b87"),
    version = 5L,
    email = "testUserF@example.com",
    username = "testUserF",
    hash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestF",
    surname = "UserF",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserG = User(
    id = UUID("c4d94896-7e1b-45fa-bae7-4fb3a89a4d63"),
    version = 6L,
    email = "testUserG@example.com",
    username = "testUserG",
    hash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestG",
    surname = "UserG",
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserH = User(
    id = UUID("5099a6b4-8809-400d-8e38-0119184d0f93"),
    version = 7L,
    email = "testUserH@example.com",
    username = "testUserH",
    hash = Some("$s0$100801$LmS/oJ7gIulUSr4qJ9by2A==$c91t4yMA594s092V4LB89topw5Deo10BXowjW3WmWjo="),
    givenname = "TestH",
    surname = "UserH",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ROLES */
  val testRoleA = Role(
    id = UUID("1430e950-77f9-4b30-baf8-bb226fc7091a"),
    version = 1L,
    name = "test role A",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleB = Role(
    id = UUID("a011504c-d118-40cd-b9eb-6e10d5738c67"),
    version = 2L,
    name = "test role B",
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleC = Role(
    id = UUID("31a4c2e6-762a-4303-bbb8-e64c24048920"),
    version = 3L,
    name = "test role C",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
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
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleG = Role(
    id = UUID("45cc7cc8-9876-45f5-8efe-ccd4dba7ea69"),
    version = 5L,
    name = "test role G",
    createdAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 18, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleH = Role(
    id = UUID("2a3edf38-750a-46aa-8428-9fb08e648ee8"),
    version = 6L,
    name = "test role H",
    createdAt = new DateTime(2014, 8, 19, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 20, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )


  /* COURSES */
  val testCourseA = Course(
    id = UUID("217c5622-ff9e-4372-8e6a-95fb3bae300b"),
    version = 1L,
    teacherId = testUserA.id,
    name = "test course A",
    color = new Color(24, 6, 8),
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCourseB = Course(
    id = UUID("404c800a-5385-4e6b-867e-365a1e6b00de"),
    version = 2L,
    teacherId = testUserB.id,
    name = "test course B",
    color = new Color(34, 8, 16),
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB
   */
  val testCourseC = Course(
    id = UUID("7cf524fa-aa7f-4bfe-93d7-8cd7787fd030"),
    teacherId = testUserA.id,
    name = "unexisting course C",
    color = new Color(24, 6, 8)
  )

  val testCourseD = Course(
    id = UUID("94cc65bb-4542-4f62-8e08-d58522e7b5f1"),
    version = 3L,
    teacherId = testUserF.id,
    name = "test course D",
    color = new Color(4, 28, 56),
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB for insert
   */
  val testCourseE = Course(
    id = UUID("d0b05b14-4a5f-4727-ac43-bd8671aab53c"),
    teacherId = testUserA.id,
    name = "unexisting course E",
    color = new Color(45, 10, 15)
  )

  val testCourseF = Course(
    id = UUID("287b61f5-da6b-4de7-8535-3bc500cffac7"),
    version = 4L,
    teacherId = testUserF.id,
    name = "test course F",
    color = new Color(4, 28, 56),
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   *  Course without students
   */
  val testCourseG = Course(
    id = UUID("b24abba8-e6c7-4700-900c-e66ed0185a70"),
    version = 5L,
    teacherId = testUserF.id,
    name = "test course G",
    color = new Color(23, 6, 45),
    createdAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 18, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )


  /* TASKS */
  /* LONG ANSWER TASKS */
  val testLongAnswerTaskA = LongAnswerTask(
    id = UUID("bf1a6ed0-9f83-4cb4-85c1-ad456299b3a3"),
    version = 1L,
    partId = UUID("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 10,
    settings = CommonTaskSettings(
      dependencyId = None,
      title = "test longAnswerTask A",
      description = "test longAnswerTask A description",
      notesAllowed = true
    ),
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testLongAnswerTaskF = LongAnswerTask(
    id = UUID("6a50a2f1-0138-4d5c-abdd-fe85a7a520a4"),
    version = 2L,
    partId = UUID("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 11,
    settings = CommonTaskSettings(
      dependencyId = None,
      title = "test longAnswerTask F",
      description = "test longAnswerTask F description",
      notesAllowed = false
    ),
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* SHORT ANSWER TASKS */
  val testShortAnswerTaskB = ShortAnswerTask(
    id = UUID("10ef05ee-7b49-4352-b86e-70510adf617f"),
    version = 2L,
    partId = UUID("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 11,
    settings = CommonTaskSettings(
      dependencyId = Option(testLongAnswerTaskA.id),
      title = "test shortAnswerTask B",
      description = "test shortAnswerTask B description",
      notesAllowed = true
    ),
    maxLength = 51,
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testShortAnswerTaskG = ShortAnswerTask(
    id = UUID("54353246-bcc4-43af-9dbd-720c61f67b8f"),
    version = 3L,
    partId = UUID("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 12,
    settings = CommonTaskSettings(
      dependencyId = Option(testLongAnswerTaskA.id),
      title = "test shortAnswerTask G",
      description = "test shortAnswerTask G description",
      notesAllowed = false
    ),
    maxLength = 52,
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MULTIPLE CHOICE TASKS */
  val testMultipleChoiceTaskC = MultipleChoiceTask(
    id = UUID("76cc2ed7-611b-4daf-aa3f-20efe42a65a0"),
    version = 3L,
    partId = UUID("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 12,
    settings = CommonTaskSettings(
      dependencyId = Option(testLongAnswerTaskA.id),
      title = "test MultipleChoiceTask C",
      description = "test MultipleChoiceTask C description",
      notesAllowed = true
    ),
    choices = Vector("choice 1", "choice 2"),
    answers  = Vector(1, 2),
    allowMultiple = false,
    randomizeChoices = true,
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testMultipleChoiceTaskH = MultipleChoiceTask(
    id = UUID("f23fca57-c6b0-4e21-9809-f68659639a7f"),
    version = 4L,
    partId = UUID("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 13,
    settings = CommonTaskSettings(
      dependencyId = Option(testLongAnswerTaskA.id),
      title = "test MultipleChoiceTask H",
      description = "test MultipleChoiceTask H description",
      notesAllowed = true
    ),
    choices = Vector("choice 3", "choice 4"),
    answers  = Vector(3, 4),
    allowMultiple = true,
    randomizeChoices = false,
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ORDERING TASKS */
  val testOrderingTaskD = OrderingTask(
    id = UUID("80840083-8923-476f-a873-8ba6c55e30c8"),
    version = 4L,
    partId = UUID("abb84847-a3d2-47a0-ae7d-8ce04063afc7"), // testPartB.id
    position = 13,
    settings = CommonTaskSettings(
      dependencyId = Option(testLongAnswerTaskA.id),
      title = "test OrderingTask D",
      description = "test OrderingTask D description",
      notesAllowed = true
    ),
    elements = Vector("element 3", "element 4"),
    answers  = Vector(3, 4),
    randomizeChoices = true,
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testOrderingTaskI = OrderingTask(
    id = UUID("3064a825-9d40-427f-9579-86e601115730"),
    version = 5L,
    partId = UUID("abb84847-a3d2-47a0-ae7d-8ce04063afc7"), // testPartB.id
    position = 14,
    settings = CommonTaskSettings(
      dependencyId = Option(testLongAnswerTaskA.id),
      title = "test OrderingTask I",
      description = "test OrderingTask I description",
      notesAllowed = false
    ),
    elements = Vector("element 5", "element 6"),
    answers  = Vector(5, 6),
    randomizeChoices = false,
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MATCHING TASKS */
  val testMatchingTaskE = MatchingTask(
    id = UUID("468a35bf-baf8-4045-aa18-4688f4d0721f"),
    version = 5L,
    partId = UUID("fb01f11b-7f23-41c8-877b-68410be62aa5"), // testPartC.id
    position = 14,
    settings = CommonTaskSettings(
      dependencyId = Option(testLongAnswerTaskA.id),
      title = "test MatchingTask E",
      description = "test MatchingTask E description",
      notesAllowed = true
    ),
    elementsLeft = Vector("choice left 5", "choice left 6"),
    elementsRight = Vector("choice right 7", "choice right 8"),
    answers  = Vector(Match(5, 6), Match(7, 8)),
    randomizeChoices = true,
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testMatchingTaskJ = MatchingTask(
    id = UUID("e8b09639-6167-41f7-8b95-fe41705e078c"),
    version = 6L,
    partId = UUID("fb01f11b-7f23-41c8-877b-68410be62aa5"), // testPartC.id
    position = 15,
    settings = CommonTaskSettings(
      dependencyId = Option(testLongAnswerTaskA.id),
      title = "test MatchingTask J",
      description = "test MatchingTask J description",
      notesAllowed = false
    ),
    elementsLeft = Vector("choice left 6", "choice left 7"),
    elementsRight = Vector("choice right 8", "choice right 9"),
    answers  = Vector(Match(6, 7), Match(8, 9)),
    randomizeChoices = true,
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testMatchingTaskK = MatchingTask(
    id = UUID("337fa731-3685-4ba3-8668-280c0096514c"),
    version = 7L,
    partId = UUID("c850ec53-f0a9-460d-918a-5e6fd538f376"), // testPartE.id
    position = 16,
    settings = CommonTaskSettings(
      dependencyId = Option(testLongAnswerTaskA.id),
      title = "test MatchingTask K",
      description = "test MatchingTask K description",
      notesAllowed = true
    ),
    elementsLeft = Vector("choice left 6", "choice left 7"),
    elementsRight = Vector("choice right 8", "choice right 9"),
    answers  = Vector(Match(6, 7), Match(8, 9)),
    randomizeChoices = true,
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )


  /* DOCUMENTS */
  val testDocumentA = Document(
    id = UUID("fd923b3f-6dc2-472e-8ce7-7a8fcc6a1a20"),
    version = 1L,
    title = "testDocumentA title",
    plaintext = "testDocumentA plaintext",
    delta = Delta(IndexedSeq.empty[Operation]),
    owner = testUserC,
    editors = Vector(),
    createdAt = Option(new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04"))),
    updatedAt = Option(new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04")))
  )


  /* WORK */
  /* LONG_ANSWER_WORK */
  val testLongAnswerWorkA = LongAnswerWork(
    id = UUID("441374e2-0b16-43ec-adb9-6a3251081d24"),
    studentId = testUserC.id,
    taskId = testLongAnswerTaskA.id,
    documentId = testDocumentA.id,
    version = 1L,
    response = "testLongAnswerWorkA response",
    isComplete = true,
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )
  /* SHORT_ANSWER_WORK */
  /* MULTIPLE_CHOICE_WORK */
  /* ORDERING_WORK */
  /* MATCHING_WORK */


  /* COMPONENTS */
  /* TEXT COMPONENT */
  val testTextComponentA = TextComponent(
    id = UUID("8cfc6089-8129-4c2e-9ed1-45d38077d438"),
    version = 1L,
    ownerId = testUserA.id,
    title = "testTextComponentA title",
    questions = "testTextComponentA questions",
    thingsToThinkAbout = "testTextComponentA thingsToThinkAbout",
    content = "testTextComponentA content",
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testTextComponentG = TextComponent(
    id = UUID("2b1357e7-1e3b-4d98-b4fd-7ff09dc77b40"),
    version = 1L,
    ownerId = testUserF.id,
    title = "testTextComponentG title",
    questions = "testTextComponentG questions",
    thingsToThinkAbout = "testTextComponentG thingsToThinkAbout",
    content = "testTextComponentG content",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* VIDEO COMPONENT */
  val testVideoComponentB = VideoComponent(
    id = UUID("50d07485-f33c-4755-9ccf-59d823cbb79e"),
    version = 2L,
    ownerId = testUserA.id,
    title = "testVideoComponentB title",
    questions = "testVideoComponentB questions",
    thingsToThinkAbout = "testVideoComponentB thingsToThinkAbout",
    vimeoId = "19579282",
    width = 640,
    height = 480,
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testVideoComponentF = VideoComponent(
    id = UUID("913e9192-eb37-4557-9833-393e964472df"),
    version = 1L,
    ownerId = testUserB.id,
    title = "testVideoComponentF title",
    questions = "testVideoComponentF questions",
    thingsToThinkAbout = "testVideoComponentF thingsToThinkAbout",
    vimeoId = "19579283",
    width = 640,
    height = 480,
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* AUDIO COMPONENT */
  val testAudioComponentC = AudioComponent(
    id = UUID("a51c6b53-5180-416d-aa77-1cc620dee9c0"),
    version = 3L,
    ownerId = testUserA.id,
    title = "testAudioComponentC title",
    questions = "testAudioComponentC questions",
    thingsToThinkAbout = "testAudioComponentC thingsToThinkAbout",
    soundcloudId = "dj-whisky-ft-nozipho-just",
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testAudioComponentD = AudioComponent(
    id = UUID("bf8980bb-8fda-49ab-a5d2-a9f537de90b0"),
    version = 1L,
    ownerId = testUserF.id,
    title = "testAudioComponentD title",
    questions = "testAudioComponentD questions",
    thingsToThinkAbout = "testAudioComponentD thingsToThinkAbout",
    soundcloudId = "fetty-wap-my-way-feat-monty",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testAudioComponentE = AudioComponent(
    id = UUID("9f2dd973-397b-4f55-9618-b0ff3af69ecb"),
    version = 4L,
    ownerId = testUserB.id,
    title = "testAudioComponentE title",
    questions = "testAudioComponentE questions",
    thingsToThinkAbout = "testAudioComponentE thingsToThinkAbout",
    soundcloudId = "revolution-radio-network",
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )


  /* PARTS */
  val testPartA = Part(
    id = UUID("5cd214be-6bba-47fa-9f35-0eb8bafec397"),
    version = 1L,
    projectId = UUID("c9b4cfce-aed4-48fd-94f5-c980763dfddc"), // testProjectA.id,
    name = "test part A",
    enabled = true,
    position = 10,
    tasks = Vector(testLongAnswerTaskA, testShortAnswerTaskB, testMultipleChoiceTaskC),
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartB = Part(
    id = UUID("abb84847-a3d2-47a0-ae7d-8ce04063afc7"),
    version = 2L,
    projectId = UUID("c9b4cfce-aed4-48fd-94f5-c980763dfddc"), // testProjectA.id,
    name = "test part B",
    enabled = false,
    position = 11,
    tasks = Vector(testOrderingTaskD, testMatchingTaskE),
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartC = Part(
    id = UUID("fb01f11b-7f23-41c8-877b-68410be62aa5"),
    version = 3L,
    projectId = UUID("e4ae3b90-9871-4339-b05c-8d39e3aaf65d"), // testProjectB.id,
    name = "test part C",
    enabled = true,
    position = 12,
    tasks = Vector(),
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB, for insert
   */
  val testPartD = Part(
    id = UUID("0229c34a-7504-468c-a061-6095b64ea7ec"),
    projectId = UUID("e4ae3b90-9871-4339-b05c-8d39e3aaf65d"), // testProjectB.id,
    name = "test part D",
    tasks = Vector()
  )

  val testPartE = Part(
    id = UUID("c850ec53-f0a9-460d-918a-5e6fd538f376"),
    version = 4L,
    projectId = UUID("4ac4d872-451b-4092-b13f-643d6d5fa930"), // testProjectC.id,
    name = "test part E",
    enabled = false,
    position = 13,
    tasks = Vector(testMatchingTaskK),
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartF = Part(
    id = UUID("e8d52684-6afd-48e5-8049-a179e8868432"),
    version = 5L,
    projectId = UUID("4ac4d872-451b-4092-b13f-643d6d5fa930"), // testProjectC.id,
    name = "test part F",
    enabled = true,
    position = 14,
    tasks = Vector(),
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartG = Part(
    id = UUID("8e080c00-2b20-4e7b-b18c-2582d79e7e68"),
    version = 6L,
    projectId = UUID("c9b4cfce-aed4-48fd-94f5-c980763dfddc"), // testProjectA.id,
    name = "test part G",
    enabled = true,
    position = 15,
    tasks = Vector(),
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartH = Part(
    id = UUID("45a146b3-fd9a-4cab-9d1d-3e9b0b15e12c"),
    version = 7L,
    projectId = UUID("4ac4d872-451b-4092-b13f-643d6d5fa930"), // testProjectC.id,
    name = "test part H",
    enabled = true,
    position = 16,
    tasks = Vector(),
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )


  /* PROJECTS */
  val testProjectA = Project(
    id = UUID("c9b4cfce-aed4-48fd-94f5-c980763dfddc"),
    courseId = testCourseA.id,
    version = 1L,
    name = "test project A",
    slug = "test project slug A",
    description = "test project A description",
    availability = "any",
    parts = Vector(testPartA, testPartB),
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testProjectB = Project(
    id = UUID("e4ae3b90-9871-4339-b05c-8d39e3aaf65d"),
    courseId = testCourseB.id,
    version = 2L,
    name = "test project B",
    slug = "test project slug B",
    description = "test project B description",
    availability = "free",
    parts = Vector(testPartC),
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testProjectC = Project(
    id = UUID("4ac4d872-451b-4092-b13f-643d6d5fa930"),
    courseId = testCourseB.id,
    version = 3L,
    name = "test project C",
    slug = "test project slug C",
    description = "test project C description",
    availability = "course",
    parts = Vector(),
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB for insert
   */
  val testProjectD = Project(
    id = UUID("00743ada-1d3a-4912-adc8-fb8a0b1b7443"),
    courseId = testCourseA.id,
    name = "test project D",
    slug = "test project slug D",
    description = "test project D description",
    availability = "course",
    parts = Vector()
  )

  /**
   * No references in other tables
   */
  val testProjectE = Project(
    id = UUID("b36919cb-2df0-43b7-bb7f-36cae797deaa"),
    courseId = testCourseA.id,
    version = 4L,
    name = "test project E",
    slug = "test project slug E",
    description = "test project E description",
    availability = "course",
    parts = Vector(),
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )


  //  /* SCHEDULES */
  //  /* Because in db there is time zone 4 for startTime and endTime, here hours should be -1 */
  //  val testCourseScheduleA = ClassSchedule(
  //    id = UUID("308792b2-7a29-43c8-ad51-a5c4f306cdaf"),
  //    courseId = testCourseA.id,
  //    version = 1L,
  //    startTime = new LocalTime(13, 38, 19),
  //    //    length = 12345,
  //    //    reason = "test ClassSchedule A reason",
  //
  //    day = new LocalDate(2015, 1, 15),
  //    description = "test ClassSchedule A description",
  //    endTime = new LocalTime(14, 38, 19),
  //
  //    createdAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04")),
  //    updatedAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  //  )
  //
  //  val testCourseScheduleB = ClassSchedule(
  //    id = UUID("dc1190c2-b5fd-4bac-95fa-7d67e1f1d445"),
  //    courseId = testCourseB.id,
  //    version = 2L,
  //    startTime = new LocalTime(11, 38, 19),
  ////    length = 12345,
  ////    reason = "test ClassSchedule B reason",
  //
  //    day = new LocalDate(2015, 1, 16),
  //    description = "test ClassSchedule B description",
  //    endTime = new LocalTime(12, 38, 19),
  //
  //    createdAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04")),
  //    updatedAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  //  )
  //
  //  val testCourseScheduleC = ClassSchedule(
  //    id = UUID("6df9d164-b151-4c38-9acd-6b91301a199d"),
  //    courseId = testCourseB.id,
  //    version = 3L,
  //    startTime = new LocalTime(15, 38, 19),
  ////    length = 12345,
  ////    reason = "test ClassSchedule C reason",
  //
  //    day = new LocalDate(2015, 1, 17),
  //    description = "test ClassSchedule C description",
  //    endTime = new LocalTime(16, 38, 19),
  //
  //    createdAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04")),
  //    updatedAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  //  )
  //
  //  /**
  //   * No data in DB for insert
  //   */
  //  val testCourseScheduleD = ClassSchedule(
  //    id = UUID("02eaad44-5f0a-4c75-b05a-c92991903c10"),
  //    courseId = testCourseB.id,
  //    startTime = new LocalTime(16, 38, 19),
  ////    length = new LocalTime(17, 38, 19),
  ////    reason = "test ClassSchedule D reason"
  //
  //    day = new LocalDate(2015, 1, 18),
  //    description = "test ClassSchedule D description",
  //    endTime = new LocalTime(17, 38, 19)
  //
  //  )
  //
  //  /**
  //   * No data in DB
  //   */
  //  val testCourseScheduleE = ClassSchedule(
  //    id = UUID("43ac00c3-7546-41f0-bc93-72cc81158597"),
  //    courseId = testCourseB.id,
  //    startTime = new LocalTime(17, 38, 19),
  ////    length = 12345,
  ////    reason = "test ClassSchedule E reason"
  //
  //    day = new LocalDate(2015, 1, 19),
  //    description = "test ClassSchedule A description",
  //    endTime = new LocalTime(18, 38, 19)
  //
  //  )
  //
  //
  //  /* SCHEDULE EXCEPTIONS */
  //  val testSectionScheduleExceptionA = SectionScheduleException(
  //    id = UUID("da17e24a-a545-4d74-94e1-427896e13ebe"),
  //    userId = testUserA.id,
  //    courseId = testCourseA.id,
  //    version = 1L,
  //    day = new LocalDate(2014, 8, 1),
  //    startTime = new LocalTime(14, 1, 19),
  //    endTime = new LocalTime(15, 1, 19),
  //    reason = "testSectionScheduleExceptionA reason",
  //    createdAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04")),
  //    updatedAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  //  )
  //
  //  val testSectionScheduleExceptionB = SectionScheduleException(
  //    id = UUID("3a285f0c-66d0-41b2-851b-cfcd203550d9"),
  //    userId = testUserB.id,
  //    courseId = testCourseB.id,
  //    version = 2L,
  //    day = new LocalDate(2014, 8, 4),
  //    startTime = new LocalTime(16, 1, 19),
  //    endTime = new LocalTime(17, 1, 19),
  //    reason = "testSectionScheduleExceptionB reason",
  //    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
  //    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  //  )
  //
  //  val testSectionScheduleExceptionC = SectionScheduleException(
  //    id = UUID("4d7ca313-f216-4f59-85ae-88bcbca70317"),
  //    userId = testUserE.id,
  //    courseId = testCourseB.id,
  //    version = 3L,
  //    day = new LocalDate(2014, 8, 7),
  //    startTime = new LocalTime(10, 1, 19),
  //    endTime = new LocalTime(11, 1, 19),
  //    reason = "testSectionScheduleExceptionC reason",
  //    createdAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04")),
  //    updatedAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  //  )
  //
  //  /**
  //   * No data in DB
  //   */
  //  val testSectionScheduleExceptionD = SectionScheduleException(
  //    id = UUID("848c8b4f-f566-4d7f-8a16-b4a76107778a"),
  //    userId = testUserE.id,
  //    courseId = testCourseB.id,
  //    version = 4L,
  //    day = new LocalDate(2014, 8, 8),
  //    startTime = new LocalTime(12, 1, 19),
  //    endTime = new LocalTime(13, 1, 19),
  //    reason = "testSectionScheduleExceptionD reason",
  //    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
  //    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  //  )
  //
  //  /**
  //   * No data in DB for insert
  //   */
  //  val testSectionScheduleExceptionE = SectionScheduleException(
  //    id = UUID("3e0ba0ec-8427-4ff1-8fe0-2aaeda67ae36"),
  //    userId = testUserE.id,
  //    courseId = testCourseB.id,
  //    day = new LocalDate(2014, 8, 9),
  //    startTime = new LocalTime(14, 1, 19),
  //    endTime = new LocalTime(15, 1, 19),
  //    reason = "testSectionScheduleExceptionE reason"
  //  )
}

