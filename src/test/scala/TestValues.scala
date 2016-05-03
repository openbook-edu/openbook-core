import java.awt.Color
import ca.shiftfocus.krispii.core.models.JournalEntry._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.document.{ Revision, Document }
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.models.tasks.questions._
import java.util.UUID
import ca.shiftfocus.krispii.core.models.work._
import org.joda.time.{ LocalTime, LocalDate, DateTime, DateTimeZone }
import ws.kahn.ot._

object TestValues {
  /* ---------------------- USERS ---------------------- */

  val testUserA = User(
    id = UUID.fromString("36c8c0ca-50aa-4806-afa5-916a5e33a81f"),
    version = 1L,
    email = "testUserA@example.com",
    username = "testUserA",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="), // adminpass
    givenname = "TestA",
    surname = "UserA",
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserB = User(
    id = UUID.fromString("6c0e29bd-d05b-4b29-8115-6be93e936c59"),
    version = 2L,
    email = "testUserB@example.com",
    username = "testUserB",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "TestB",
    surname = "UserB",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * User has no references in other tables
   */
  val testUserC = User(
    id = UUID.fromString("f5f98407-3a0b-4ea5-952a-575886e90586"),
    version = 3L,
    email = "testUserC@example.com",
    username = "testUserC",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "TestC",
    surname = "UserC",
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB
   */
  val testUserD = User(
    id = UUID.fromString("4d97f26c-df3f-4866-8919-11f51f14e9c4"),
    email = "testUserD@example.com",
    username = "testUserD",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "TestD",
    surname = "UserD"
  )

  val testUserE = User(
    id = UUID.fromString("871b5250-6712-4e54-8ab6-0784cae0bc64"),
    version = 4L,
    email = "testUserE@example.com",
    username = "testUserE",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "TestE",
    surname = "UserE",
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserF = User(
    id = UUID.fromString("4d01347e-c592-4e5f-b09f-dd281b3d9b87"),
    version = 5L,
    email = "testUserF@example.com",
    username = "testUserF",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "TestF",
    surname = "UserF",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserG = User(
    id = UUID.fromString("c4d94896-7e1b-45fa-bae7-4fb3a89a4d63"),
    version = 6L,
    email = "testUserG@example.com",
    username = "testUserG",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "TestG",
    surname = "UserG",
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserH = User(
    id = UUID.fromString("5099a6b4-8809-400d-8e38-0119184d0f93"),
    version = 7L,
    email = "testUserH@example.com",
    username = "testUserH",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "TestH",
    surname = "UserH",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserI = User(
    id = UUID.fromString("8b6dc674-d1ae-11e5-9080-08626681851d"),
    version = 1L,
    email = "rafael@krispii.com",
    username = "rafaelya",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "Rafael",
    surname = "Yanez",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserJ = User(
    id = UUID.fromString("7c62a526-d1b0-11e5-9080-08626681851d"),
    version = 1L,
    email = "yanez@krispii.com",
    username = "yanez",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "Antonio",
    surname = "Yanez",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserX = User(
    id = UUID.fromString("a898c83a-5638-4483-9528-8037b3ed661d"),
    version = 1L,
    email = "kmccormick@krispii.com",
    username = "mysterion",
    hash = Some("$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE="),
    givenname = "Kenny",
    surname = "McCormick",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )
  /* ---------------------- ROLES ---------------------- */

  val testRoleAuth = Role(
    id = UUID.fromString("40b178e1-3e91-47b9-9377-6fa74fa17293"),
    version = 1L,
    name = "authenticated",
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleAdmin = Role(
    id = UUID.fromString("8acf3355-31ef-4bac-a046-00e3574412ad"),
    version = 2L,
    name = "administrator",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleTeacher = Role(
    id = UUID.fromString("ca8a1d7f-de06-45c8-b52e-8e9c7ead2e7e"),
    version = 2L,
    name = "teacher",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleStudent = Role(
    id = UUID.fromString("4ef48c1f-5446-4748-a9d5-2565472945b3"),
    version = 2L,
    name = "student",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleA = Role(
    id = UUID.fromString("1430e950-77f9-4b30-baf8-bb226fc7091a"),
    version = 1L,
    name = "test role A",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleB = Role(
    id = UUID.fromString("a011504c-d118-40cd-b9eb-6e10d5738c67"),
    version = 2L,
    name = "test role B",
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleC = Role(
    id = UUID.fromString("31a4c2e6-762a-4303-bbb8-e64c24048920"),
    version = 3L,
    name = "test role C",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB
   */
  val testRoleD = Role(
    id = UUID.fromString("b82d356d-a1bb-4e07-b28f-d15060fb42c2"),
    name = "test role D"
  )

  /**
   * No data in DB
   */
  val testRoleE = Role(
    id = UUID.fromString("29a84d7b-f90a-4a26-a224-b70631fdfbe4"),
    name = "test role E"
  )

  val testRoleF = Role(
    id = UUID.fromString("45b35527-07ad-4c4f-9051-f0e755216163"),
    version = 4L,
    name = "test role F",
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleG = Role(
    id = UUID.fromString("45cc7cc8-9876-45f5-8efe-ccd4dba7ea69"),
    version = 5L,
    name = "test role G",
    createdAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 18, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRoleH = Role(
    id = UUID.fromString("2a3edf38-750a-46aa-8428-9fb08e648ee8"),
    version = 6L,
    name = "test role H",
    createdAt = new DateTime(2014, 8, 19, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 20, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- COURSES ---------------------- */

  val testCourseA = Course(
    id = UUID.fromString("217c5622-ff9e-4372-8e6a-95fb3bae300b"),
    version = 1L,
    teacherId = testUserA.id,
    name = "test course A",
    color = new Color(24, 6, 8),
    slug = "test-course-A-slug",
    chatEnabled = true,
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCourseB = Course(
    id = UUID.fromString("404c800a-5385-4e6b-867e-365a1e6b00de"),
    version = 2L,
    teacherId = testUserB.id,
    name = "test course B",
    color = new Color(34, 8, 16),
    slug = "test-course-B-slug",
    chatEnabled = true,
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB
   */
  val testCourseC = Course(
    id = UUID.fromString("7cf524fa-aa7f-4bfe-93d7-8cd7787fd030"),
    teacherId = testUserA.id,
    name = "unexisting course C",
    color = new Color(24, 6, 8),
    slug = "test-course-C-slug",
    chatEnabled = true
  )

  val testCourseD = Course(
    id = UUID.fromString("94cc65bb-4542-4f62-8e08-d58522e7b5f1"),
    version = 3L,
    teacherId = testUserF.id,
    name = "test course D",
    color = new Color(4, 28, 56),
    slug = "test-course-D-slug",
    chatEnabled = true,
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB for insert
   */
  val testCourseE = Course(
    id = UUID.fromString("d0b05b14-4a5f-4727-ac43-bd8671aab53c"),
    teacherId = testUserA.id,
    name = "unexisting course E",
    color = new Color(45, 10, 15),
    slug = "test-course-E-slug",
    chatEnabled = true
  )

  val testCourseF = Course(
    id = UUID.fromString("287b61f5-da6b-4de7-8535-3bc500cffac7"),
    version = 4L,
    teacherId = testUserF.id,
    name = "test course F",
    color = new Color(4, 28, 56),
    slug = "test-course-F-slug",
    chatEnabled = true,
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   *  Course without students
   */
  val testCourseG = Course(
    id = UUID.fromString("b24abba8-e6c7-4700-900c-e66ed0185a70"),
    version = 5L,
    teacherId = testUserF.id,
    name = "test course G",
    color = new Color(23, 6, 45),
    slug = "test-course-G-slug",
    chatEnabled = true,
    createdAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 18, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCourseH = Course(
    //4ac4d872-451b-4092-b13f-643d6d5fa931
    id = UUID.fromString("4ac4d872-451b-4092-b13f-643d6d5fa931"),
    version = 5L,
    teacherId = testUserF.id,
    name = "test course H",
    color = new Color(23, 6, 45),
    slug = "test-course-H-slug",
    chatEnabled = true,
    createdAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 18, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCourseK = Course(
    id = UUID.fromString("b24abba8-e6c7-4700-900c-e66ed0185a71"),
    version = 5L,
    teacherId = testUserF.id,
    name = "test course K",
    color = new Color(23, 6, 45),
    slug = "test-course-K-slug",
    chatEnabled = true,
    createdAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 18, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- QUESTIONS ---------------------- */

  val testShortQuestionA = ShortQuestion(
    id = UUID.fromString("9d2ed6e8-ccdd-474f-9583-4d10eafaa2a6"),
    title = "testShortQuestionA title",
    description = "testShortQuestionA description",
    maxLength = 51
  )

  val testMultipleChoiceQuestionB = MultipleChoiceQuestion(
    id = UUID.fromString("d7a0c042-4d4b-4f31-bc19-59a2b6659e3c"),
    title = "testMultipleChoiceQuestionB title",
    description = "testMultipleChoiceQuestionB description",
    choices = IndexedSeq("choice 1", "choice 2"),
    correct = IndexedSeq(1, 2),
    singleAnswer = true
  )

  val testOrderingQuestionC = OrderingQuestion(
    id = UUID.fromString("667a8b1c-3230-43d4-bd7b-2b150205b109"),
    title = "testOrderingQuestionC title",
    description = "testOrderingQuestionC description",
    choices = IndexedSeq("choice 3", "choice 4")
  )

  val testMatchingQuestionD = MatchingQuestion(
    id = UUID.fromString("1646f580-2347-4cdd-8b7d-fd43588a3a50"),
    title = "testMatchingQuestionD title",
    description = "testMatchingQuestionD description",
    choices = IndexedSeq(MatchingQuestion.Match("choice left 5", "choice right 6"), MatchingQuestion.Match("choice left 7", "choice right 8"))
  )

  val testBlanksQuestionE = BlanksQuestion(
    id = UUID.fromString("67a68639-0172-4948-88f0-57e77a7502d7"),
    title = "testBlanksQuestionE title",
    description = "testBlanksQuestionE description",
    text = "testBlanksQuestionE text",
    inputs = IndexedSeq(
      BlanksQuestion.Blank(5, 50),
      BlanksQuestion.Blank(6, 60)
    )
  )

  /* ---------------------- TASKS ---------------------- */
  CommonTaskSettings()
  /* LONG ANSWER TASKS */
  val testLongAnswerTaskA = DocumentTask(
    id = UUID.fromString("bf1a6ed0-9f83-4cb4-85c1-ad456299b3a3"),
    version = 1L,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 10,
    settings = CommonTaskSettings(
      title = "test longAnswerTask A",
      description = "test longAnswerTask A description",
      notesAllowed = true,
      help = "test help text testLongAnswerTaskA",
      notesTitle = Some("test longAnswerTask A notes title"),
      responseTitle = Some("test longAnswerTask A response title")
    ),
    dependencyId = None,
    maxGrade = "55",
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  // Position 26 for TaskRepository.findNow and .findNowFromAll, so it should be skiped
  val testLongAnswerTaskN = DocumentTask(
    id = UUID.fromString("73b75cd0-c796-429f-a734-02fabca367aa"),
    version = 2L,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 26,
    settings = CommonTaskSettings(
      title = "test longAnswerTask N",
      description = "test longAnswerTask N description",
      notesAllowed = true,
      help = "test help text testLongAnswerTaskN",
      notesTitle = Some("test longAnswerTask N notes title"),
      responseTitle = Some("test longAnswerTask N response title")
    ),
    dependencyId = None,
    maxGrade = "25",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  // Position 27 for TaskRepository.findNow and .findNowFromAll, so it should be skiped
  val testLongAnswerTaskO = DocumentTask(
    id = UUID.fromString("b807dbf4-b4fe-4b7b-b1b7-11083899470d"),
    version = 3L,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 27,
    settings = CommonTaskSettings(
      title = "test longAnswerTask O",
      description = "test longAnswerTask O description",
      notesAllowed = true,
      help = "test help text testLongAnswerTaskO",
      notesTitle = Some("test longAnswerTask O notes title"),
      responseTitle = Some("test longAnswerTask O response title")
    ),
    dependencyId = Some(testLongAnswerTaskN.id),
    maxGrade = "15",
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testLongAnswerTaskF = DocumentTask(
    id = UUID.fromString("6a50a2f1-0138-4d5c-abdd-fe85a7a520a4"),
    version = 2L,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 13,
    settings = CommonTaskSettings(
      title = "test longAnswerTask F",
      description = "test longAnswerTask F description",
      notesAllowed = false,
      help = "test help text testLongAnswerTaskF",
      notesTitle = Some("test longAnswerTask F notes title"),
      responseTitle = Some("test longAnswerTask F response title")
    ),
    dependencyId = Some(testLongAnswerTaskA.id),
    maxGrade = "5",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* SHORT ANSWER TASKS */
  val testShortAnswerTaskB = QuestionTask(
    id = UUID.fromString("10ef05ee-7b49-4352-b86e-70510adf617f"),
    version = 2L,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 14,
    settings = CommonTaskSettings(
      title = "test shortAnswerTask B",
      description = "test shortAnswerTask B description",
      notesAllowed = true,
      help = "test help text testShortAnswerTaskB",
      notesTitle = Some("test shortAnswerTask B notes title"),
      responseTitle = Some("test shortAnswerTask B response title")
    ),
    questions = IndexedSeq(testShortQuestionA),
    maxGrade = "90",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testShortAnswerTaskG = QuestionTask(
    id = UUID.fromString("54353246-bcc4-43af-9dbd-720c61f67b8f"),
    version = 3L,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 15,
    settings = CommonTaskSettings(
      title = "test shortAnswerTask G",
      description = "test shortAnswerTask G description",
      notesAllowed = false,
      help = "test help text testShortAnswerTaskG",
      notesTitle = Some("test shortAnswerTask G notes title"),
      responseTitle = Some("test shortAnswerTask G response title")
    ),
    questions = IndexedSeq(testShortQuestionA),
    maxGrade = "90",
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MULTIPLE CHOICE TASKS */
  val testMultipleChoiceTaskC = QuestionTask(
    id = UUID.fromString("76cc2ed7-611b-4daf-aa3f-20efe42a65a0"),
    version = 3L,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 16,
    settings = CommonTaskSettings(
      title = "test MultipleChoiceTask C",
      description = "test MultipleChoiceTask C description",
      notesAllowed = true,
      help = "test help text testMultipleChoiceTaskC",
      notesTitle = Some("test MultipleChoiceTask C notes title"),
      responseTitle = Some("test MultipleChoiceTask C response title")
    ),
    questions = IndexedSeq(testMultipleChoiceQuestionB),
    maxGrade = "30",
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testMultipleChoiceTaskH = QuestionTask(
    id = UUID.fromString("f23fca57-c6b0-4e21-9809-f68659639a7f"),
    version = 4L,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 17,
    settings = CommonTaskSettings(
      title = "test MultipleChoiceTask H",
      description = "test MultipleChoiceTask H description",
      notesAllowed = true,
      help = "test help text testMultipleChoiceTaskH",
      notesTitle = Some("test MultipleChoiceTask H notes title"),
      responseTitle = Some("test MultipleChoiceTask H response title")
    ),
    questions = IndexedSeq(testMultipleChoiceQuestionB),
    maxGrade = "20",
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ORDERING TASKS */
  val testOrderingTaskD = QuestionTask(
    id = UUID.fromString("80840083-8923-476f-a873-8ba6c55e30c8"),
    version = 4L,
    partId = UUID.fromString("abb84847-a3d2-47a0-ae7d-8ce04063afc7"), // testPartB.id
    position = 18,
    settings = CommonTaskSettings(
      title = "test OrderingTask D",
      description = "test OrderingTask D description",
      notesAllowed = true,
      help = "test help text testOrderingTaskD",
      notesTitle = Some("test OrderingTask D notes title"),
      responseTitle = Some("test OrderingTask D response title")
    ),
    questions = IndexedSeq(testOrderingQuestionC),
    maxGrade = "1",
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testOrderingTaskL = QuestionTask(
    id = UUID.fromString("3d3578bd-60d3-4aea-be07-0359dad2fecb"),
    version = 6L,
    partId = UUID.fromString("abb84847-a3d2-47a0-ae7d-8ce04063afc7"), // testPartB.id
    position = 19,
    settings = CommonTaskSettings(
      title = "test OrderingTask L",
      description = "test OrderingTask L description",
      notesAllowed = true,
      help = "test help text testOrderingTaskL",
      notesTitle = Some("test OrderingTask L notes title"),
      responseTitle = Some("test OrderingTask L response title")
    ),
    questions = IndexedSeq(testOrderingQuestionC),
    maxGrade = "77",
    createdAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testOrderingTaskN = QuestionTask(
    id = UUID.fromString("599a78ad-5bff-4246-9835-32fcb41168a6"),
    version = 7L,
    partId = UUID.fromString("8e080c00-2b20-4e7b-b18c-2582d79e7e68"), // testPartG.id
    position = 20,
    settings = CommonTaskSettings(
      title = "test OrderingTask N",
      description = "test OrderingTask N description",
      notesAllowed = true,
      help = "test help text testOrderingTaskN",
      notesTitle = Some("test OrderingTask N notes title"),
      responseTitle = Some("test OrderingTask N response title")
    ),
    questions = IndexedSeq(testOrderingQuestionC),
    maxGrade = "1000",
    createdAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testOrderingTaskI = QuestionTask(
    id = UUID.fromString("3064a825-9d40-427f-9579-86e601115730"),
    version = 5L,
    partId = UUID.fromString("abb84847-a3d2-47a0-ae7d-8ce04063afc7"), // testPartB.id
    position = 21,
    settings = CommonTaskSettings(
      title = "test OrderingTask I",
      description = "test OrderingTask I description",
      notesAllowed = false,
      help = "test help text testOrderingTaskI",
      notesTitle = Some("test OrderingTask I notes title"),
      responseTitle = Some("test OrderingTask I response title")
    ),
    questions = IndexedSeq(testOrderingQuestionC),
    maxGrade = "10",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MATCHING TASKS */
  // Position 1 for TaskRepository.findNow (is in another project, so should be skiped) .findNowFromAll (should be selected)
  val testMatchingTaskE = QuestionTask(
    id = UUID.fromString("468a35bf-baf8-4045-aa18-4688f4d0721f"),
    version = 5L,
    partId = UUID.fromString("fb01f11b-7f23-41c8-877b-68410be62aa5"), // testPartC.id
    position = 1,
    settings = CommonTaskSettings(
      title = "test MatchingTask E",
      description = "test MatchingTask E description",
      notesAllowed = true,
      help = "test help text testMatchingTaskE",
      notesTitle = Some("test MatchingTask E notes title"),
      responseTitle = Some("test MatchingTask E response title")
    ),
    questions = IndexedSeq(testMatchingQuestionD),
    maxGrade = "5",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testMatchingTaskJ = QuestionTask(
    id = UUID.fromString("e8b09639-6167-41f7-8b95-fe41705e078c"),
    version = 6L,
    partId = UUID.fromString("fb01f11b-7f23-41c8-877b-68410be62aa5"), // testPartC.id
    position = 23,
    settings = CommonTaskSettings(
      title = "test MatchingTask J",
      description = "test MatchingTask J description",
      notesAllowed = false,
      help = "test help text testMatchingTaskJ",
      notesTitle = Some("test MatchingTask J notes title"),
      responseTitle = Some("test MatchingTask J response title")
    ),
    questions = IndexedSeq(testMatchingQuestionD),
    maxGrade = "100",
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testMatchingTaskK = QuestionTask(
    id = UUID.fromString("337fa731-3685-4ba3-8668-280c0096514c"),
    version = 7L,
    partId = UUID.fromString("abb84847-a3d2-47a0-ae7d-8ce04063afc7"), // testPartB.id
    position = 24,
    settings = CommonTaskSettings(
      title = "test MatchingTask K",
      description = "test MatchingTask K description",
      notesAllowed = true,
      help = "test help text testMatchingTaskK",
      notesTitle = Some("test MatchingTask K notes title"),
      responseTitle = Some("test MatchingTask K response title")
    ),
    questions = IndexedSeq(testMatchingQuestionD),
    maxGrade = "0",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testMatchingTaskM = QuestionTask(
    id = UUID.fromString("129f2b08-56d3-4e14-aa5b-659f53f71e39"),
    version = 8L,
    partId = UUID.fromString("abb84847-a3d2-47a0-ae7d-8ce04063afc7"), // testPartB.id
    position = 25,
    settings = CommonTaskSettings(
      title = "test MatchingTask M",
      description = "test MatchingTask M description",
      notesAllowed = true,
      help = "test help text testMatchingTaskM",
      notesTitle = Some("test MatchingTask M notes title"),
      responseTitle = Some("test MatchingTask M response title")
    ),
    questions = IndexedSeq(testMatchingQuestionD),
    maxGrade = "0",
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* BLANKS TASKS */
  val testBlanksTaskP = QuestionTask(
    id = UUID.fromString("9a258f44-4ee8-4bab-855b-53966e53ca10"),
    version = 9L,
    partId = UUID.fromString("8e080c00-2b20-4e7b-b18c-2582d79e7e68"), // testPartG.id
    position = 28,
    settings = CommonTaskSettings(
      title = "test BlanksTask P",
      description = "test BlanksTask P description",
      notesAllowed = true,
      help = "test help text testBlanksTaskP",
      notesTitle = Some("test BlanksTask P notes title"),
      responseTitle = Some("test BlanksTask P response title")
    ),
    questions = IndexedSeq(testBlanksQuestionE, testMatchingQuestionD),
    maxGrade = "0",
    createdAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 18, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testBlanksTaskQ = QuestionTask(
    id = UUID.fromString("4801f8a5-0d64-4e53-9dac-ebc06159b8cf"),
    version = 10L,
    partId = UUID.fromString("8e080c00-2b20-4e7b-b18c-2582d79e7e68"), // testPartG.id
    position = 29,
    settings = CommonTaskSettings(
      title = "test BlanksTask Q",
      description = "test BlanksTask Q description",
      notesAllowed = true,
      help = "test help text testBlanksTaskQ",
      notesTitle = Some("test BlanksTask Q notes title"),
      responseTitle = Some("test BlanksTask Q response title")
    ),
    questions = IndexedSeq(testBlanksQuestionE, testMatchingQuestionD),
    maxGrade = "0",
    createdAt = new DateTime(2014, 8, 19, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 20, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MEDIA TASKS */

  val testMediaTaskA = MediaTask(
    id = UUID.fromString("a7121b74-eac1-11e5-9ce9-5e5517507c66"),
    version = 3,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 16,
    settings = CommonTaskSettings(
      title = "test MediaTask A",
      description = "test MediaTask A description",
      notesAllowed = true,
      help = "test help text testMediaTaskA",
      notesTitle = Some("test MediaTask A notes title"),
      responseTitle = Some("test MediaTask A response title")
    ),
    mediaType = 0,
    maxGrade = "0",
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testMediaTaskB = MediaTask(
    id = UUID.fromString("46a994fa-eac2-11e5-9ce9-5e5517507c66"),
    version = 8L,
    partId = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"), // testPartA.id
    position = 25,
    settings = CommonTaskSettings(
      title = "test testMediaTask B",
      description = "test testMediaTask B description",
      notesAllowed = true,
      help = "test help text testMediaTaskB",
      notesTitle = Some("test testMediaTask B notes title"),
      responseTitle = Some("test testMediaTask B response title")
    ),
    mediaType = 2,
    maxGrade = "10",
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )




  /* ---------------------- DOCUMENTS ---------------------- */

  val testDocumentA = Document(
    id = UUID.fromString("fd923b3f-6dc2-472e-8ce7-7a8fcc6a1a20"),
    version = 2L,
    title = "testDocumentA title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Sam")
    )),
    ownerId = testUserC.id,
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentB = Document(
    id = UUID.fromString("15173757-b881-4440-8285-4e3d2c03616a"),
    version = 2L,
    title = "testDocumentB title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Dean")
    )),
    ownerId = testUserE.id,
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentC = Document(
    id = UUID.fromString("462b7f6c-8b62-4c99-8643-a63b2720b2a7"),
    version = 2L,
    title = "testDocumentC title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Jhonatan")
    )),
    ownerId = testUserE.id,
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentD = Document(
    id = UUID.fromString("bd01c988-0369-4dda-ada2-05a9ff3645cf"),
    version = 2L,
    title = "testDocumentD title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Morgan")
    )),
    ownerId = testUserC.id,
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testDocumentE = Document(
    id = UUID.fromString("41d47dd9-e550-4942-af74-962ec26a0995"),
    version = 1L,
    title = "testDocumentE title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Bruno")
    )),
    ownerId = testUserC.id,
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  // --- Documents for TaskFeedbacks ---

  val testDocumentF = Document(
    id = UUID.fromString("1a9d5407-b3c4-44a1-8e7e-1d7e9578eabc"),
    version = 2L,
    title = "testDocumentF title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Jason")
    )),
    ownerId = testUserA.id,
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentG = Document(
    id = UUID.fromString("300ddfb7-f9bf-47fe-a0b2-26f332828fff"),
    version = 2L,
    title = "testDocumentG title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Moony")
    )),
    ownerId = testUserA.id,
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentH = Document(
    id = UUID.fromString("eb8ef353-d22f-48a4-a356-351e0de3ed16"),
    version = 2L,
    title = "testDocumentH title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Flipper")
    )),
    ownerId = testUserB.id,
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentI = Document(
    id = UUID.fromString("9110c16f-45fd-4211-9e39-b15ab8b6f9ee"),
    version = 2L,
    title = "testDocumentI title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Groovy")
    )),
    ownerId = testUserB.id,
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentJ = Document(
    id = UUID.fromString("30739c6d-4377-4a2f-8aa3-d1240dfb0740"),
    version = 2L,
    title = "testDocumentJ title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Bobby")
    )),
    ownerId = testUserA.id,
    createdAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 19, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  // --- Documents for TaskScratchpads ---

  val testDocumentK = Document(
    id = UUID.fromString("2f1180f0-17f4-488b-9f03-ad8fbfbeaf3a"),
    version = 2L,
    title = "testDocumentK title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Moris")
    )),
    ownerId = testUserC.id,
    createdAt = new DateTime(2014, 8, 19, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 21, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentL = Document(
    id = UUID.fromString("7c9d0dae-fe79-4ecc-b36c-c141a4122fab"),
    version = 2L,
    title = "testDocumentL title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Boris")
    )),
    ownerId = testUserE.id,
    createdAt = new DateTime(2014, 8, 21, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 23, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentM = Document(
    id = UUID.fromString("0ed856aa-fd4c-486d-b6c5-293ca18c37dd"),
    version = 2L,
    title = "testDocumentM title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Vasea")
    )),
    ownerId = testUserC.id,
    createdAt = new DateTime(2014, 8, 23, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 25, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentN = Document(
    id = UUID.fromString("196a1793-c688-4f66-b725-a8353dd1ac67"),
    version = 2L,
    title = "testDocumentN title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Petea")
    )),
    ownerId = testUserC.id,
    createdAt = new DateTime(2014, 8, 25, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 27, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testDocumentO = Document(
    id = UUID.fromString("78b9baaf-16b7-43a3-9cec-410104cdde4e"),
    version = 2L,
    title = "testDocumentO title",
    delta = Delta(IndexedSeq(
      InsertText("Hello Doris")
    )),
    ownerId = testUserE.id,
    createdAt = new DateTime(2014, 8, 27, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 29, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- REVISIONS---------------------- */

  val testCurrentRevisionA = Revision(
    documentId = testDocumentA.id,
    version = testDocumentA.version,
    authorId = testDocumentA.ownerId,
    delta = Delta(IndexedSeq(
      Delete(7),
      InsertText("Hello"),
      Retain(4)
    )),
    createdAt = testDocumentA.updatedAt
  )

  val testPreviousRevisionA = Revision(
    documentId = testDocumentA.id,
    version = testDocumentA.version - 1,
    authorId = testDocumentA.ownerId,
    delta = Delta(IndexedSeq(
      InsertText("Goodbye Sam")
    )),
    createdAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCurrentRevisionB = Revision(
    documentId = testDocumentB.id,
    version = testDocumentB.version,
    authorId = testDocumentB.ownerId,
    delta = Delta(IndexedSeq(
      Delete(7),
      InsertText("Hello"),
      Retain(5)
    )),
    createdAt = testDocumentB.updatedAt
  )

  val testPreviousRevisionB = Revision(
    documentId = testDocumentB.id,
    version = testDocumentB.version - 1,
    authorId = testDocumentB.ownerId,
    delta = Delta(IndexedSeq(
      InsertText("Goodbye Dean")
    )),
    createdAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCurrentRevisionC = Revision(
    documentId = testDocumentC.id,
    version = testDocumentC.version,
    authorId = testDocumentC.ownerId,
    delta = Delta(IndexedSeq(
      Delete(7),
      InsertText("Hello"),
      Retain(9)
    )),
    createdAt = testDocumentC.updatedAt
  )

  val testPreviousRevisionC = Revision(
    documentId = testDocumentC.id,
    version = testDocumentC.version - 1,
    authorId = testDocumentC.ownerId,
    delta = Delta(IndexedSeq(
      InsertText("Goodbye Jhonatan")
    )),
    createdAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCurrentRevisionD = Revision(
    documentId = testDocumentD.id,
    version = testDocumentD.version,
    authorId = testDocumentD.ownerId,
    delta = Delta(IndexedSeq(
      Delete(7),
      InsertText("Hello"),
      Retain(7)
    )),
    createdAt = testDocumentD.updatedAt
  )

  val testPreviousRevisionD = Revision(
    documentId = testDocumentD.id,
    version = testDocumentD.version - 1,
    authorId = testDocumentD.ownerId,
    delta = Delta(IndexedSeq(
      InsertText("Goodbye Morgan")
    )),
    createdAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUnexistingRevisionD = Revision(
    documentId = testDocumentD.id,
    version = testDocumentD.version + 1,
    authorId = testDocumentD.ownerId,
    delta = Delta(IndexedSeq(
      Retain(6),
      Delete(6),
      InsertText("Chris")
    )),
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- DOCUMENT_REVISIONS---------------------- */

  val testDocumentRevisionA = Document(
    id = testDocumentA.id,
    version = testPreviousRevisionA.version,
    title = testDocumentA.title,
    delta = testPreviousRevisionA.delta,
    ownerId = testDocumentA.ownerId,
    createdAt = testDocumentA.createdAt,
    updatedAt = testPreviousRevisionA.createdAt
  )

  val testDocumentRevisionB = Document(
    id = testDocumentB.id,
    version = testPreviousRevisionB.version,
    title = testDocumentB.title,
    delta = testPreviousRevisionB.delta,
    ownerId = testDocumentB.ownerId,
    createdAt = testDocumentB.createdAt,
    updatedAt = testPreviousRevisionB.createdAt
  )

  val testDocumentRevisionC = Document(
    id = testDocumentC.id,
    version = testPreviousRevisionC.version,
    title = testDocumentC.title,
    delta = testPreviousRevisionC.delta,
    ownerId = testDocumentC.ownerId,
    createdAt = testDocumentC.createdAt,
    updatedAt = testPreviousRevisionC.createdAt
  )

  val testDocumentRevisionD = Document(
    id = testDocumentD.id,
    version = testPreviousRevisionD.version,
    title = testDocumentD.title,
    delta = testPreviousRevisionD.delta,
    ownerId = testDocumentD.ownerId,
    createdAt = testDocumentD.createdAt,
    updatedAt = testPreviousRevisionD.createdAt
  )

  /* ---------------------- TASK_FEEDBACKS ---------------------- */

  val testTaskFeedbackA = TaskFeedback(
    studentId = testUserC.id,
    taskId = testLongAnswerTaskA.id,
    version = testDocumentF.version,
    documentId = testDocumentF.id,
    createdAt = testDocumentF.createdAt,
    updatedAt = testDocumentF.updatedAt
  )

  val testTaskFeedbackB = TaskFeedback(
    studentId = testUserE.id,
    taskId = testShortAnswerTaskB.id,
    version = testDocumentG.version,
    documentId = testDocumentG.id,
    createdAt = testDocumentG.createdAt,
    updatedAt = testDocumentG.updatedAt
  )

  val testTaskFeedbackC = TaskFeedback(
    studentId = testUserC.id,
    taskId = testMatchingTaskE.id,
    version = testDocumentH.version,
    documentId = testDocumentH.id,
    createdAt = testDocumentH.createdAt,
    updatedAt = testDocumentH.updatedAt
  )

  val testTaskFeedbackD = TaskFeedback(
    studentId = testUserE.id,
    taskId = testMatchingTaskE.id,
    version = testDocumentI.version,
    documentId = testDocumentI.id,
    createdAt = testDocumentI.createdAt,
    updatedAt = testDocumentI.updatedAt
  )

  val testTaskFeedbackE = TaskFeedback(
    studentId = testUserC.id,
    taskId = testOrderingTaskN.id,
    version = testDocumentJ.version,
    documentId = testDocumentJ.id,
    createdAt = testDocumentJ.createdAt,
    updatedAt = testDocumentJ.updatedAt
  )

  /* No data in DB */
  val testTaskFeedbackF = TaskFeedback(
    studentId = testUserE.id,
    taskId = testOrderingTaskL.id,
    version = testDocumentA.version,
    documentId = testDocumentA.id,
    createdAt = testDocumentA.createdAt,
    updatedAt = testDocumentA.updatedAt
  )

  /* ---------------------- TASK_SCRATCHPADS ---------------------- */

  val testTaskScratchpadA = TaskScratchpad(
    userId = testUserC.id,
    taskId = testLongAnswerTaskA.id,
    version = testDocumentK.version,
    documentId = testDocumentK.id,
    createdAt = testDocumentK.createdAt,
    updatedAt = testDocumentK.updatedAt
  )

  val testTaskScratchpadB = TaskScratchpad(
    userId = testUserE.id,
    taskId = testShortAnswerTaskB.id,
    version = testDocumentL.version,
    documentId = testDocumentL.id,
    createdAt = testDocumentL.createdAt,
    updatedAt = testDocumentL.updatedAt
  )

  val testTaskScratchpadC = TaskScratchpad(
    userId = testUserC.id,
    taskId = testMatchingTaskE.id,
    version = testDocumentN.version,
    documentId = testDocumentN.id,
    createdAt = testDocumentN.createdAt,
    updatedAt = testDocumentN.updatedAt
  )

  val testTaskScratchpadD = TaskScratchpad(
    userId = testUserE.id,
    taskId = testMatchingTaskE.id,
    version = testDocumentO.version,
    documentId = testDocumentO.id,
    createdAt = testDocumentO.createdAt,
    updatedAt = testDocumentO.updatedAt
  )

  val testTaskScratchpadE = TaskScratchpad(
    userId = testUserC.id,
    taskId = testOrderingTaskN.id,
    version = testDocumentM.version,
    documentId = testDocumentM.id,
    createdAt = testDocumentM.createdAt,
    updatedAt = testDocumentM.updatedAt
  )

  /* No data in DB */
  val testTaskScratchpadF = TaskScratchpad(
    userId = testUserE.id,
    taskId = testOrderingTaskL.id,
    version = testDocumentA.version,
    documentId = testDocumentA.id,
    createdAt = testDocumentA.createdAt,
    updatedAt = testDocumentA.updatedAt
  )

  /* ---------------------- ANSWERS ---------------------- */

  val testShortAnswerA = ShortAnswer(
    answer = "testShortAnswerA answer"
  )

  val testMultipleChoiceAnswerB = MultipleChoiceAnswer(
    answer = IndexedSeq(1, 2)
  )

  val testOrderingAnswerC = OrderingAnswer(
    answer = IndexedSeq(2, 3)
  )

  val testMatchingAnswerD = MatchingAnswer(
    answer = IndexedSeq(MatchingAnswer.Match(2, 3), MatchingAnswer.Match(4, 5))
  )

  val testMatchingAnswerF = MatchingAnswer(
    answer = IndexedSeq(MatchingAnswer.Match(6, 7), MatchingAnswer.Match(8, 9))
  )

  val testBlanksAnswerE = BlanksAnswer(
    answer = IndexedSeq(
      "testBlanksAnswerE answer one",
      "testBlanksAnswerE answer two"
    )
  )

  /* ---------------------- ANSWERS REVISIONS---------------------- */

  val testShortAnswerRevisionA = ShortAnswer(
    answer = "testShortAnswerRevisionA answer"
  )

  val testMultipleChoiceAnswerRevisionB = MultipleChoiceAnswer(
    answer = IndexedSeq(3, 4)
  )

  val testOrderingAnswerRevisionC = OrderingAnswer(
    answer = IndexedSeq(5, 6)
  )

  val testMatchingAnswerRevisionD = MatchingAnswer(
    answer = IndexedSeq(MatchingAnswer.Match(6, 7), MatchingAnswer.Match(8, 9))
  )

  val testBlanksAnswerRevisionE = BlanksAnswer(
    answer = IndexedSeq(
      "testBlanksAnswerRevisionE answer one",
      "testBlanksAnswerRevisionE answer two"
    )
  )

  /* ---------------------- WORK ---------------------- */

  /* LONG_ANSWER_WORK */
  val testLongAnswerWorkA = DocumentWork(
    id = UUID.fromString("441374e2-0b16-43ec-adb9-6a3251081d24"),
    studentId = testUserC.id,
    taskId = testLongAnswerTaskA.id,
    documentId = testDocumentA.id,
    version = testDocumentA.version,
    response = Some(testDocumentA),
    isComplete = true,
    grade = "",
    createdAt = testDocumentA.createdAt,
    updatedAt = testDocumentA.updatedAt
  )

  val testLongAnswerWorkF = DocumentWork(
    id = UUID.fromString("f7fcffc3-7b79-4de7-b6dd-cf37aa155fd9"),
    studentId = testUserE.id,
    taskId = testLongAnswerTaskA.id,
    documentId = testDocumentB.id,
    version = testDocumentB.version,
    response = Some(testDocumentB),
    isComplete = true,
    grade = "",
    createdAt = testDocumentB.createdAt,
    updatedAt = testDocumentB.updatedAt
  )

  /* No data in DB */
  val testLongAnswerWorkK = DocumentWork(
    id = UUID.fromString("db4f6062-0cb6-4b0d-87db-e7dcb7ab8ffc"),
    studentId = testUserG.id,
    taskId = testLongAnswerTaskA.id,
    documentId = testDocumentB.id,
    version = 1L,
    response = Some(testDocumentB),
    isComplete = true,
    grade = "",
    createdAt = testDocumentB.createdAt,
    updatedAt = testDocumentB.updatedAt
  )

  /* SHORT_ANSWER_WORK */
  val testShortAnswerWorkB = QuestionWork(
    id = UUID.fromString("cbf452cd-915a-4b24-9d02-92be013bbba8"),
    studentId = testUserE.id,
    taskId = testShortAnswerTaskB.id,
    version = 2,
    response = Answers(Map(
      testShortQuestionA.id -> testShortAnswerA
    )),
    isComplete = false,
    grade = "",
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testShortAnswerWorkG = QuestionWork(
    id = UUID.fromString("b7bb09c1-6aca-40de-8152-5da483a5c476"),
    studentId = testUserC.id,
    taskId = testShortAnswerTaskB.id,
    version = 2,
    response = Answers(Map(
      testShortQuestionA.id -> testShortAnswerA
    )),
    isComplete = false,
    grade = "",
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testShortAnswerWorkL = QuestionWork(
    id = UUID.fromString("f88108f3-e4b1-47de-a88e-3fbc19d92adb"),
    studentId = testUserG.id,
    taskId = testShortAnswerTaskB.id,
    version = 1L,
    grade = "",
    response = Answers(Map(
      testShortQuestionA.id -> testShortAnswerA
    )),
    isComplete = false,
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MULTIPLE_CHOICE_WORK */
  val testMultipleChoiceWorkC = QuestionWork(
    id = UUID.fromString("edfd6198-97b0-4f21-9e15-fbe4ed051970"),
    studentId = testUserC.id,
    taskId = testMultipleChoiceTaskC.id,
    version = 3L,
    grade = "",
    response = Answers(Map(
      testMultipleChoiceQuestionB.id -> testMultipleChoiceAnswerB
    )),
    isComplete = true,
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testMultipleChoiceWorkH = QuestionWork(
    id = UUID.fromString("8f3b9f09-db43-4670-b159-0763eb4eaecd"),
    studentId = testUserE.id,
    taskId = testMultipleChoiceTaskC.id,
    version = 8L,
    response = Answers(Map(
      testMultipleChoiceQuestionB.id -> testMultipleChoiceAnswerB
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testMultipleChoiceWorkM = QuestionWork(
    id = UUID.fromString("7746b9ac-e9b8-4115-bb2f-d7ecb630663f"),
    studentId = testUserG.id,
    taskId = testMultipleChoiceTaskC.id,
    version = 1L,
    response = Answers(Map(
      testMultipleChoiceQuestionB.id -> testMultipleChoiceAnswerB
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ORDERING_WORK */
  val testOrderingWorkD = QuestionWork(
    id = UUID.fromString("125eef5a-7e89-441c-b138-c1803bafdc03"),
    studentId = testUserC.id,
    taskId = testOrderingTaskN.id,
    version = 4L,
    response = Answers(Map(
      testOrderingQuestionC.id -> testOrderingAnswerC
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testOrderingWorkI = QuestionWork(
    id = UUID.fromString("db5165f4-4d48-4007-9191-beecd77763c7"),
    studentId = testUserE.id,
    taskId = testOrderingTaskN.id,
    version = 5L,
    response = Answers(Map(
      testOrderingQuestionC.id -> testOrderingAnswerC
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testOrderingWorkN = QuestionWork(
    id = UUID.fromString("7746b9ac-e9b8-4115-bb2f-d7ecb630663f"),
    studentId = testUserG.id,
    taskId = testOrderingTaskN.id,
    version = 1L,
    response = Answers(Map(
      testOrderingQuestionC.id -> testOrderingAnswerC
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MATCHING_WORK */
  val testMatchingWorkE = QuestionWork(
    id = UUID.fromString("e47442dd-8ac9-4d06-ad6f-ef62720d4ed3"),
    studentId = testUserC.id,
    taskId = testMatchingTaskE.id,
    version = 5L,
    response = Answers(Map(
      testMatchingQuestionD.id -> testMatchingAnswerD
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testMatchingWorkJ = QuestionWork(
    id = UUID.fromString("c57e0335-51da-4144-9dfc-dfa97f5f1a7c"),
    studentId = testUserE.id,
    taskId = testMatchingTaskE.id,
    version = 6L,
    response = Answers(Map(
      testMatchingQuestionD.id -> testMatchingAnswerD
    )),
    isComplete = false,
    grade = "",
    createdAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testMatchingWorkO = QuestionWork(
    id = UUID.fromString("f285d7ff-8f2a-46be-89c9-8869d28efc9d"),
    studentId = testUserG.id,
    taskId = testMatchingTaskE.id,
    version = 1L,
    response = Answers(Map(
      testMatchingQuestionD.id -> testMatchingAnswerD
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* BLANKS_WORK */
  val testBlanksWorkK = QuestionWork(
    id = UUID.fromString("507cd2f2-4869-48f1-ab19-812c6a1ef5ed"),
    studentId = testUserC.id,
    taskId = testBlanksTaskP.id,
    version = 4L,
    response = Answers(Map(
      testBlanksQuestionE.id -> testBlanksAnswerE
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testBlanksWorkL = QuestionWork(
    id = UUID.fromString("f9293872-1eb0-4523-8697-05898ebc8746"),
    studentId = testUserE.id,
    taskId = testBlanksTaskP.id,
    version = 5L,
    response = Answers(Map(
      testBlanksQuestionE.id -> testBlanksAnswerE
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testBlanksWorkM = QuestionWork(
    id = UUID.fromString("c25ebe84-0ebf-4fbe-9410-79b436b3fd86"),
    studentId = testUserG.id,
    taskId = testBlanksTaskP.id,
    version = 6L,
    response = Answers(Map(
      testBlanksQuestionE.id -> testBlanksAnswerE
    )),
    isComplete = true,
    grade = "",
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 17, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MEDIA_WORK */
  val testMediaWorkA = MediaWork(
    id = UUID.fromString("38800ed0-8c6e-482b-84e5-806c1f86316d"),
    studentId = testUserC.id,
    taskId = testMediaTaskA.id,
    version = 3L,
    fileData = MediaAnswer(fileName = Some("image.jpg")),
    isComplete = false,
    grade = "",
    createdAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testMediaWorkB = MediaWork(
    id = UUID.fromString("761f5ba1-7379-4fbd-80aa-94d39a6d8cb7"),
    studentId = testUserE.id,
    taskId = testMediaTaskA.id,
    version = 1L,
    fileData = MediaAnswer(),
    isComplete = false,
    grade = "",
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- WORK REVISIONS ---------------------- */

  /* LONG_ANSWER_WORK */
  val testLongAnswerWorkRevisionA = DocumentWork(
    id = testLongAnswerWorkA.id,
    studentId = testLongAnswerWorkA.studentId,
    taskId = testLongAnswerWorkA.taskId,
    documentId = testDocumentRevisionA.id,
    version = testDocumentRevisionA.version,
    response = Some(testDocumentRevisionA),
    isComplete = testLongAnswerWorkA.isComplete,
    grade = "",
    createdAt = testLongAnswerWorkA.createdAt,
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testLongAnswerWorkRevisionF = DocumentWork(
    id = testLongAnswerWorkF.id,
    studentId = testLongAnswerWorkF.studentId,
    taskId = testLongAnswerWorkF.taskId,
    documentId = testDocumentRevisionB.id,
    version = testDocumentRevisionB.version,
    response = Some(testDocumentRevisionB),
    isComplete = testLongAnswerWorkF.isComplete,
    grade = "",
    createdAt = testLongAnswerWorkF.createdAt,
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* SHORT_ANSWER_WORK */
  val testShortAnswerWorkRevisionB = QuestionWork(
    id = testShortAnswerWorkB.id,
    studentId = testShortAnswerWorkB.studentId,
    taskId = testShortAnswerWorkB.taskId,
    version = testShortAnswerWorkB.version - 1,
    response = Answers(Map(
      testShortQuestionA.id -> testShortAnswerRevisionA
    )),
    isComplete = testShortAnswerWorkB.isComplete,
    grade = "",
    createdAt = testShortAnswerWorkB.createdAt,
    updatedAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testShortAnswerWorkRevisionG = QuestionWork(
    id = testShortAnswerWorkG.id,
    studentId = testShortAnswerWorkG.studentId,
    taskId = testShortAnswerWorkG.taskId,
    version = testShortAnswerWorkG.version - 1,
    grade = "",
    response = Answers(Map(
      testShortQuestionA.id -> testShortAnswerRevisionA
    )),
    isComplete = testShortAnswerWorkG.isComplete,
    createdAt = testShortAnswerWorkG.createdAt,
    updatedAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MULTIPLE_CHOICE_WORK REVISIONS */
  val testMultipleChoiceWorkRevisionC = QuestionWork(
    id = testMultipleChoiceWorkC.id,
    studentId = testMultipleChoiceWorkC.studentId,
    taskId = testMultipleChoiceWorkC.taskId,
    version = testMultipleChoiceWorkC.version - 1,
    grade = "",
    response = Answers(Map(
      testMultipleChoiceQuestionB.id -> testMultipleChoiceAnswerRevisionB
    )),
    isComplete = testMultipleChoiceWorkC.isComplete,
    createdAt = testMultipleChoiceWorkC.createdAt,
    updatedAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testMultipleChoiceWorkRevisionH = QuestionWork(
    id = testMultipleChoiceWorkH.id,
    studentId = testMultipleChoiceWorkH.studentId,
    taskId = testMultipleChoiceWorkH.taskId,
    version = testMultipleChoiceWorkH.version - 1,
    grade = "",
    response = Answers(Map(
      testMultipleChoiceQuestionB.id -> testMultipleChoiceAnswerRevisionB
    )),
    isComplete = testMultipleChoiceWorkH.isComplete,
    createdAt = testMultipleChoiceWorkH.createdAt,
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ORDERING_WORK REVISIONS */
  val testOrderingWorkRevisionD = QuestionWork(
    id = testOrderingWorkD.id,
    studentId = testOrderingWorkD.studentId,
    taskId = testOrderingWorkD.taskId,
    version = testOrderingWorkD.version - 1,
    response = Answers(Map(
      testOrderingQuestionC.id -> testOrderingAnswerRevisionC
    )),
    isComplete = testOrderingWorkD.isComplete,
    grade = "",
    createdAt = testOrderingWorkD.createdAt,
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testOrderingWorkRevisionI = QuestionWork(
    id = testOrderingWorkI.id,
    studentId = testOrderingWorkI.studentId,
    taskId = testOrderingWorkI.taskId,
    version = testOrderingWorkI.version - 1,
    grade = "",
    response = Answers(Map(
      testOrderingQuestionC.id -> testOrderingAnswerRevisionC
    )),
    isComplete = testOrderingWorkI.isComplete,
    createdAt = testOrderingWorkI.createdAt,
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* MATCHING_WORK REVISIONS */
  val testMatchingWorkRevisionE = QuestionWork(
    id = testMatchingWorkE.id,
    studentId = testMatchingWorkE.studentId,
    taskId = testMatchingWorkE.taskId,
    version = testMatchingWorkE.version - 1,
    grade = "",
    response = Answers(Map(
      testMatchingQuestionD.id -> testMatchingAnswerRevisionD
    )),
    isComplete = testMatchingWorkE.isComplete,
    createdAt = testMatchingWorkE.createdAt,
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testMatchingWorkRevisionJ = QuestionWork(
    id = testMatchingWorkJ.id,
    studentId = testMatchingWorkJ.studentId,
    taskId = testMatchingWorkJ.taskId,
    grade = "",
    version = testMatchingWorkJ.version - 1,
    response = Answers(Map(
      testMatchingQuestionD.id -> testMatchingAnswerRevisionD
    )),
    isComplete = testMatchingWorkJ.isComplete,
    createdAt = testMatchingWorkJ.createdAt,
    updatedAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testMatchingWorkRevisionK = QuestionWork(
    id = testMatchingWorkJ.id,
    studentId = testMatchingWorkJ.studentId,
    taskId = testMatchingWorkJ.taskId,
    version = testMatchingWorkJ.version - 2,
    grade = "",
    response = Answers(Map(
      testMatchingQuestionD.id -> testMatchingAnswerRevisionD
    )),
    isComplete = testMatchingWorkJ.isComplete,
    createdAt = testMatchingWorkJ.createdAt,
    updatedAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* BLANKS WORK REVISIONS */
  val testBlanksWorkRevisionK = QuestionWork(
    id = testBlanksWorkK.id,
    studentId = testBlanksWorkK.studentId,
    taskId = testBlanksWorkK.taskId,
    version = testBlanksWorkK.version - 1,
    grade = "",
    response = Answers(Map(
      testBlanksQuestionE.id -> testBlanksAnswerRevisionE
    )),
    isComplete = testBlanksWorkK.isComplete,
    createdAt = testBlanksWorkK.createdAt,
    updatedAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testBlanksWorkRevisionL = QuestionWork(
    id = testBlanksWorkL.id,
    studentId = testBlanksWorkL.studentId,
    taskId = testBlanksWorkL.taskId,
    version = testBlanksWorkL.version - 1,
    grade = "",
    response = Answers(Map(
      testBlanksQuestionE.id -> testBlanksAnswerRevisionE
    )),
    isComplete = testBlanksWorkL.isComplete,
    createdAt = testBlanksWorkL.createdAt,
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- COMPONENTS ---------------------- */

  /* TEXT COMPONENT */
  val testTextComponentA = TextComponent(
    id = UUID.fromString("8cfc6089-8129-4c2e-9ed1-45d38077d438"),
    version = 1L,
    ownerId = testUserA.id,
    title = "testTextComponentA title",
    questions = "testTextComponentA questions",
    thingsToThinkAbout = "testTextComponentA thingsToThinkAbout",
    content = "testTextComponentA content",
    order = 1,
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testTextComponentG = TextComponent(
    id = UUID.fromString("2b1357e7-1e3b-4d98-b4fd-7ff09dc77b40"),
    version = 1L,
    ownerId = testUserF.id,
    title = "testTextComponentG title",
    questions = "testTextComponentG questions",
    thingsToThinkAbout = "testTextComponentG thingsToThinkAbout",
    content = "testTextComponentG content",
    order = 2,
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* VIDEO COMPONENT */
  val testVideoComponentB = VideoComponent(
    id = UUID.fromString("50d07485-f33c-4755-9ccf-59d823cbb79e"),
    version = 2L,
    ownerId = testUserA.id,
    title = "testVideoComponentB title",
    questions = "testVideoComponentB questions",
    thingsToThinkAbout = "testVideoComponentB thingsToThinkAbout",
    videoData = MediaData(Some("vimeo"), Some("19579282")),
    width = 640,
    height = 480,
    order = 3,
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testVideoComponentF = VideoComponent(
    id = UUID.fromString("913e9192-eb37-4557-9833-393e964472df"),
    version = 1L,
    ownerId = testUserB.id,
    title = "testVideoComponentF title",
    questions = "testVideoComponentF questions",
    thingsToThinkAbout = "testVideoComponentF thingsToThinkAbout",
    videoData = MediaData(Some("vimeo"), Some("19579283")),
    width = 640,
    height = 480,
    order = 0,
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* AUDIO COMPONENT */
  val testAudioComponentC = AudioComponent(
    id = UUID.fromString("a51c6b53-5180-416d-aa77-1cc620dee9c0"),
    version = 3L,
    ownerId = testUserA.id,
    title = "testAudioComponentC title",
    questions = "testAudioComponentC questions",
    thingsToThinkAbout = "testAudioComponentC thingsToThinkAbout",
    audioData = MediaData(Some("sound_cloud"), Some("dj-whisky-ft-nozipho-just")),
    order = 0,
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testAudioComponentD = AudioComponent(
    id = UUID.fromString("bf8980bb-8fda-49ab-a5d2-a9f537de90b0"),
    version = 1L,
    ownerId = testUserF.id,
    title = "testAudioComponentD title",
    questions = "testAudioComponentD questions",
    thingsToThinkAbout = "testAudioComponentD thingsToThinkAbout",
    audioData = MediaData(Some("sound_cloud"), Some("fetty-wap-my-way-feat-monty")),
    order = 0,
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testAudioComponentE = AudioComponent(
    id = UUID.fromString("9f2dd973-397b-4f55-9618-b0ff3af69ecb"),
    version = 4L,
    ownerId = testUserB.id,
    title = "testAudioComponentE title",
    questions = "testAudioComponentE questions",
    thingsToThinkAbout = "testAudioComponentE thingsToThinkAbout",
    audioData = MediaData(Some("sound_cloud"), Some("revolution-radio-network")),
    order = 0,
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )



  val testGenericHTMLComponentH = GenericHTMLComponent(
    id = UUID.fromString("8cfc6089-8129-4c2e-9ed1-45d38077d440"),
    version = 5L,
    ownerId = testUserA.id,
    title = "testGenericComponentH title",
    questions = "testGenericComponentH questions",
    thingsToThinkAbout = "testGenericComponentH thingsToThinkAbout",
    order = 0,
    htmlContent = "testGenericComponentH content",
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRubricComponentK = RubricComponent(
    id = UUID.fromString("8cfc6089-8129-4c2e-9ed1-45d38077d449"),
    version = 6L,
    ownerId = testUserA.id,
    title = "testRubricComponentK title",
    questions = "testRubricComponentK questions",
    thingsToThinkAbout = "testRubricComponentK thingsToThinkAbout",
    order = 0,
    rubricContent = "testRubricComponentK content",
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testGenericHTMLComponentI = GenericHTMLComponent(
    id = UUID.fromString("8cfc6089-8129-4c2e-9ed1-45d38077d448"),
    version = 1L,
    ownerId = testUserA.id,
    title = "testGenericComponentI title",
    questions = "testGenericComponentI questions",
    thingsToThinkAbout = "testGenericComponentI thingsToThinkAbout",
    htmlContent = "testGenericComponentI content",
    order = 0,
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testRubricComponentL = RubricComponent(
    id = UUID.fromString("fd923b3f-6dc2-472e-8ce7-7a8fcc6a1a34"),
    title = "Rubric Component",
    questions = "Some questions?",
    version = 1,
    ownerId = testUserC.id,
    thingsToThinkAbout = "Something to think about",
    order = 1,
    rubricContent =
      """
        |<!DOCTYPE html>
        |<html>
        |<head>
        |<title>Page Title</title>
        |</head>
        |<body>
        |
        |<h1>This is a Heading</h1>
        |<p>This is a paragraph.</p>
        |
        |</body>
        |</html>
      """.stripMargin,
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testGenericHTMLComponentM = RubricComponent(
    id = UUID.fromString("fd923b3f-6dc2-472e-8ce7-7a8fcc6a1a98"),
    title = "GenericHTML Component",
    questions = "Some questions?",
    version = 1,
    ownerId = testUserC.id,
    thingsToThinkAbout = "Something to think about",
    order = 2,
    rubricContent =
      """
        |<!DOCTYPE html>
        |<html>
        |<head>
        |<title>Page Title</title>
        |</head>
        |<body>
        |
        |<h1>This is a Heading</h1>
        |<p>This is a paragraph.</p>
        |
        |</body>
        |</html>
      """.stripMargin,
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- PARTS ---------------------- */

  val testPartA = Part(
    id = UUID.fromString("5cd214be-6bba-47fa-9f35-0eb8bafec397"),
    version = 1L,
    projectId = UUID.fromString("c9b4cfce-aed4-48fd-94f5-c980763dfddc"), // testProjectA.id,
    name = "test part A",
    enabled = true,
    position = 10,
    tasks = Vector(testLongAnswerTaskA, testShortAnswerTaskB, testMultipleChoiceTaskC),
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartB = Part(
    id = UUID.fromString("abb84847-a3d2-47a0-ae7d-8ce04063afc7"),
    version = 2L,
    projectId = UUID.fromString("c9b4cfce-aed4-48fd-94f5-c980763dfddc"), // testProjectA.id,
    name = "test part B",
    enabled = false,
    position = 11,
    tasks = Vector(testOrderingTaskD, testMatchingTaskE),
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartC = Part(
    id = UUID.fromString("fb01f11b-7f23-41c8-877b-68410be62aa5"),
    version = 3L,
    projectId = UUID.fromString("e4ae3b90-9871-4339-b05c-8d39e3aaf65d"), // testProjectB.id,
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
    id = UUID.fromString("0229c34a-7504-468c-a061-6095b64ea7ec"),
    projectId = UUID.fromString("e4ae3b90-9871-4339-b05c-8d39e3aaf65d"), // testProjectB.id,
    name = "test part D",
    tasks = Vector()
  )

  val testPartE = Part(
    id = UUID.fromString("c850ec53-f0a9-460d-918a-5e6fd538f376"),
    version = 4L,
    projectId = UUID.fromString("4ac4d872-451b-4092-b13f-643d6d5fa930"), // testProjectC.id,
    name = "test part E",
    enabled = false,
    position = 13,
    tasks = Vector(testMatchingTaskK),
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartF = Part(
    id = UUID.fromString("e8d52684-6afd-48e5-8049-a179e8868432"),
    version = 5L,
    projectId = UUID.fromString("4ac4d872-451b-4092-b13f-643d6d5fa930"), // testProjectC.id,
    name = "test part F",
    enabled = true,
    position = 14,
    tasks = Vector(),
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartG = Part(
    id = UUID.fromString("8e080c00-2b20-4e7b-b18c-2582d79e7e68"),
    version = 6L,
    projectId = UUID.fromString("c9b4cfce-aed4-48fd-94f5-c980763dfddc"), // testProjectA.id,
    name = "test part G",
    enabled = true,
    position = 15,
    tasks = Vector(),
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartH = Part(
    id = UUID.fromString("45a146b3-fd9a-4cab-9d1d-3e9b0b15e12c"),
    version = 7L,
    projectId = UUID.fromString("4ac4d872-451b-4092-b13f-643d6d5fa930"), // testProjectC.id,
    name = "test part H",
    enabled = true,
    position = 16,
    tasks = Vector(),
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testPartI = Part(
    id = UUID.fromString("fb01f11b-7f23-41c8-877b-68410be62ab6"),
    version = 3L,
    projectId = UUID.fromString("00743ada-1d3a-4912-adc8-fb8a0b1b7447"), // testProjectB.id,
    name = "test part I",
    enabled = true,
    position = 1,
    tasks = Vector(),
    components = Vector(TestValues.testRubricComponentL, TestValues.testGenericHTMLComponentM),
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- PROJECTS ---------------------- */

  val testProjectA = Project(
    id = UUID.fromString("c9b4cfce-aed4-48fd-94f5-c980763dfddc"),
    courseId = testCourseA.id,
    version = 1L,
    enabled = false,
    isMaster = false,
    name = "test project A",
    slug = "test-project-slug-A",
    description = "test project A description",
    availability = "any",
    parts = Vector(testPartA, testPartB, testPartG),
    createdAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testProjectB = Project(
    id = UUID.fromString("e4ae3b90-9871-4339-b05c-8d39e3aaf65d"),
    courseId = testCourseB.id,
    version = 2L,
    name = "test project B",
    slug = "test-project-slug-B",
    description = "test project B description",
    availability = "free",
    parts = Vector(testPartC),
    createdAt = new DateTime(2014, 8, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testProjectC = Project(
    id = UUID.fromString("4ac4d872-451b-4092-b13f-643d6d5fa930"),
    courseId = testCourseB.id,
    version = 3L,
    name = "test project C",
    slug = "test-project-slug-C",
    description = "test project C description",
    availability = "course",
    parts = Vector(testPartE, testPartF, testPartH),
    createdAt = new DateTime(2014, 8, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB for insert
   */
  val testProjectD = Project(
    id = UUID.fromString("00743ada-1d3a-4912-adc8-fb8a0b1b7443"),
    courseId = testCourseA.id,
    name = "test project D",
    slug = "test-project-slug-D",
    description = "test project D description",
    availability = "course",
    isMaster = false,
    parentId = null,
    parts = Vector()
  )

  /**
   * No references in other tables
   */
  val testProjectE = Project(
    id = UUID.fromString("b36919cb-2df0-43b7-bb7f-36cae797deaa"),
    courseId = testCourseA.id,
    version = 4L,
    name = "test project E",
    slug = "test-project-slug-E",
    description = "test project E description",
    availability = "course",
    parts = Vector(),
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * Master project F
   */
  val testProjectF = Project(
    id = UUID.fromString("b36919cb-2df0-43b7-bb7f-36cae797deab"),
    courseId = testCourseG.id,
    version = 1L,
    name = "test project F",
    slug = "test-project-slug-F",
    description = "test project F description",
    availability = "course",
    parts = Vector(),
    isMaster = true,
    enabled = true,
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * Master project G
   */
  val testProjectG = Project(
    id = UUID.fromString("b36919cb-2df0-43b7-bb7f-36cae797deac"),
    courseId = testCourseG.id,
    version = 1L,
    name = "test project G",
    slug = "test-project-slug-G",
    description = "test project G description",
    availability = "course",
    parts = Vector(),
    isMaster = true,
    enabled = true,
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * Master project to be used in insert test
   */
  val testProjectH = Project(
    id = UUID.fromString("00743ada-1d3a-4912-adc8-fb8a0b1b7447"),
    courseId = testCourseG.id,
    name = "test project H",
    slug = "test-project-slug-H",
    description = "test project H description",
    availability = "course",
    isMaster = true,
    parts = Vector(testPartI),
    enabled = true,
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- SCHEDULES ---------------------- */

  val testCourseScheduleA = CourseSchedule(
    id = UUID.fromString("308792b2-7a29-43c8-ad51-a5c4f306cdaf"),
    courseId = testCourseA.id,
    version = 1L,
    day = new LocalDate(2015, 1, 15),
    startTime = new LocalTime(14, 1, 19),
    endTime = new LocalTime(15, 1, 19),
    description = "test CourseSchedule A description",
    createdAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCourseScheduleB = CourseSchedule(
    id = UUID.fromString("dc1190c2-b5fd-4bac-95fa-7d67e1f1d445"),
    courseId = testCourseB.id,
    version = 2L,
    day = new LocalDate(2015, 1, 16),
    startTime = new LocalTime(16, 1, 19),
    endTime = new LocalTime(17, 1, 19),
    description = "test CourseSchedule B description",
    createdAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCourseScheduleC = CourseSchedule(
    id = UUID.fromString("6df9d164-b151-4c38-9acd-6b91301a199d"),
    courseId = testCourseB.id,
    version = 3L,
    day = new LocalDate(2015, 1, 17),
    startTime = new LocalTime(18, 1, 19),
    endTime = new LocalTime(19, 1, 19),
    description = "test CourseSchedule C description",
    createdAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB
   */
  val testCourseScheduleD = CourseSchedule(
    id = UUID.fromString("02eaad44-5f0a-4c75-b05a-c92991903c10"),
    courseId = testCourseB.id,
    day = new LocalDate(2015, 1, 18),
    startTime = new LocalTime(16, 38, 19),
    endTime = new LocalTime(17, 38, 19),
    description = "test CourseSchedule D description"
  )

  /* ---------------------- SCHEDULE EXCEPTIONS ---------------------- */

  val testCourseScheduleExceptionA = CourseScheduleException(
    id = UUID.fromString("da17e24a-a545-4d74-94e1-427896e13ebe"),
    userId = testUserC.id,
    courseId = testCourseA.id,
    version = 1L,
    day = new LocalDate(2014, 8, 1),
    startTime = new LocalTime(14, 1, 19),
    endTime = new LocalTime(15, 1, 19),
    reason = "testCourseScheduleExceptionA reason",
    createdAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCourseScheduleExceptionB = CourseScheduleException(
    id = UUID.fromString("3a285f0c-66d0-41b2-851b-cfcd203550d9"),
    userId = testUserC.id,
    courseId = testCourseB.id,
    version = 2L,
    day = new LocalDate(2014, 8, 2),
    startTime = new LocalTime(16, 1, 19),
    endTime = new LocalTime(17, 1, 19),
    reason = "testCourseScheduleExceptionB reason",
    createdAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCourseScheduleExceptionC = CourseScheduleException(
    id = UUID.fromString("4d7ca313-f216-4f59-85ae-88bcbca70317"),
    userId = testUserE.id,
    courseId = testCourseB.id,
    version = 3L,
    day = new LocalDate(2014, 8, 3),
    startTime = new LocalTime(18, 1, 19),
    endTime = new LocalTime(19, 1, 19),
    reason = "testCourseScheduleExceptionC reason",
    createdAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testCourseScheduleExceptionD = CourseScheduleException(
    id = UUID.fromString("b9a1cd29-3c04-450e-9b4a-2a63a6871c35"),
    userId = testUserE.id,
    courseId = testCourseB.id,
    version = 4L,
    day = new LocalDate(2014, 8, 4),
    startTime = new LocalTime(20, 1, 19),
    endTime = new LocalTime(21, 1, 19),
    reason = "testCourseScheduleExceptionD reason",
    createdAt = new DateTime(2014, 8, 8, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 9, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /**
   * No data in DB
   */
  val testCourseScheduleExceptionE = CourseScheduleException(
    id = UUID.fromString("848c8b4f-f566-4d7f-8a16-b4a76107778a"),
    userId = testUserE.id,
    courseId = testCourseA.id,
    day = new LocalDate(2014, 8, 5),
    startTime = new LocalTime(22, 1, 19),
    endTime = new LocalTime(23, 1, 19),
    reason = "testCourseScheduleExceptionE reason"
  )

  /* ---------------------- JOURNAL ENTRIES ---------------------- */

  val testJournalEntryA = JournalEntry(
    id = UUID.fromString("6aabd410-735f-4023-ae04-9f67f84a3846"),
    version = 1L,
    userId = testUserA.id,
    projectId = testProjectA.id,
    entryType = JournalEntryView.entryType,
    item = "item 1",
    message = "journalEntry.message",
    createdAt = new DateTime(2014, 1, 1, 13, 1, 19, 545, DateTimeZone.forID("-05")),
    updatedAt = new DateTime(2014, 1, 2, 13, 1, 19, 545, DateTimeZone.forID("-05"))
  )

  val testJournalEntryB = JournalEntry(
    id = UUID.fromString("2ec1c797-9a60-4b11-a860-259ae0f59134"),
    version = 2L,
    userId = testUserA.id,
    projectId = testProjectA.id,
    entryType = JournalEntryClick.entryType,
    item = "item 2",
    message = "journalEntry.message",
    createdAt = new DateTime(2014, 2, 3, 13, 1, 19, 545, DateTimeZone.forID("-05")),
    updatedAt = new DateTime(2014, 2, 4, 13, 1, 19, 545, DateTimeZone.forID("-05"))
  )

  val testJournalEntryC = JournalEntry(
    id = UUID.fromString("f7a21844-4c76-4711-ae13-5e8a3758cefb"),
    version = 3L,
    userId = testUserA.id,
    projectId = testProjectA.id,
    entryType = JournalEntryWatch.entryType,
    item = "item 3",
    message = "journalEntry.message",
    createdAt = new DateTime(2014, 3, 5, 13, 1, 19, 545, DateTimeZone.forID("-05")),
    updatedAt = new DateTime(2014, 3, 6, 13, 1, 19, 545, DateTimeZone.forID("-05"))
  )

  val testJournalEntryD = JournalEntry(
    id = UUID.fromString("d77a1706-e230-4798-853f-257cad2ed627"),
    version = 4L,
    userId = testUserA.id,
    projectId = testProjectA.id,
    entryType = JournalEntryListen.entryType,
    item = "item 4",
    message = "journalEntry.message",
    createdAt = new DateTime(2014, 4, 7, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 4, 8, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testJournalEntryE = JournalEntry(
    id = UUID.fromString("0d809bb8-779b-4e55-817c-f995959ff290"),
    version = 5L,
    userId = testUserB.id,
    projectId = testProjectB.id,
    entryType = JournalEntryWrite.entryType,
    item = "item 5",
    message = "journalEntry.message",
    createdAt = new DateTime(2014, 5, 9, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 5, 10, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testJournalEntryF = JournalEntry(
    id = UUID.fromString("59402102-4fb1-4fad-a0e1-605b37e09965"),
    version = 6L,
    userId = testUserB.id,
    projectId = testProjectB.id,
    entryType = JournalEntryCreate.entryType,
    item = "item 6",
    message = "journalEntry.message",
    createdAt = new DateTime(2014, 6, 11, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 6, 12, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testJournalEntryG = JournalEntry(
    id = UUID.fromString("eab8e5d1-d88c-4718-b1bf-da524daca133"),
    version = 7L,
    userId = testUserB.id,
    projectId = testProjectB.id,
    entryType = JournalEntryUpdate.entryType,
    item = "item 7",
    message = "journalEntry.message",
    createdAt = new DateTime(2014, 7, 13, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 7, 14, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testJournalEntryH = JournalEntry(
    id = UUID.fromString("453c7d39-2bbd-4041-9fb4-6fb59a134395"),
    version = 8L,
    userId = testUserB.id,
    projectId = testProjectB.id,
    entryType = JournalEntryDelete.entryType,
    item = "item 8",
    message = "journalEntry.message",
    createdAt = new DateTime(2014, 8, 15, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 8, 16, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testJournalEntryI = JournalEntry(
    id = UUID.fromString("cc19d1cd-9114-413a-96ba-46c981525e30"),
    version = 9L,
    userId = testUserA.id,
    projectId = testProjectA.id,
    entryType = JournalEntryDelete.entryType,
    item = "item 9",
    message = "journalEntry.message",
    createdAt = new DateTime(2014, 9, 17, 14, 1, 19, 545, DateTimeZone.forID("-04")),
    updatedAt = new DateTime(2014, 9, 18, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testJournalEntryJ = JournalEntry(
    id = UUID.fromString("f3bc1000-8d68-46d3-8d9a-7975ef4f65a4"),
    version = 1L,
    userId = testUserC.id,
    projectId = testProjectC.id,
    entryType = JournalEntryView.entryType,
    item = "item 10",
    message = "journalEntry.message"
  )

  /* ---------------------- CHAT_LOGS ---------------------- */

  val testChatA = Chat(
    courseId = testCourseA.id,
    messageNum = 1L,
    userId = testUserE.id,
    message = "testChatA message",
    hidden = false,
    createdAt = new DateTime(2014, 8, 1, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testChatB = Chat(
    courseId = testCourseA.id,
    messageNum = 2L,
    userId = testUserC.id,
    message = "testChatB message",
    hidden = false,
    createdAt = new DateTime(2014, 8, 2, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testChatC = Chat(
    courseId = testCourseA.id,
    messageNum = 3L,
    userId = testUserC.id,
    message = "testChatC message",
    hidden = false,
    createdAt = new DateTime(2014, 8, 3, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testChatD = Chat(
    courseId = testCourseA.id,
    messageNum = 4L,
    userId = testUserE.id,
    message = "testChatD message",
    hidden = false,
    createdAt = new DateTime(2014, 8, 4, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testChatE = Chat(
    courseId = testCourseA.id,
    messageNum = 5L,
    userId = testUserC.id,
    message = "testChatE message",
    hidden = false,
    createdAt = new DateTime(2014, 8, 5, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testChatF = Chat(
    courseId = testCourseA.id,
    messageNum = 6L,
    userId = testUserC.id,
    message = "testChatF message",
    hidden = false,
    createdAt = new DateTime(2014, 8, 6, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* No data in DB */
  val testChatG = Chat(
    courseId = testCourseA.id,
    messageNum = 7L,
    userId = testUserE.id,
    message = "testChatG message",
    hidden = false,
    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- PROJECT_SCRATCHPADS ---------------------- */

  val testProjectScratchpadA = ProjectScratchpad(
    userId = testUserA.id,
    projectId = testProjectA.id,
    version = testDocumentK.version,
    documentId = testDocumentJ.id,
    createdAt = testDocumentJ.createdAt,
    updatedAt = testDocumentJ.updatedAt
  )

  val testProjectScratchpadB = ProjectScratchpad(
    userId = testUserA.id,
    projectId = testProjectB.id,
    version = testDocumentF.version,
    documentId = testDocumentF.id,
    createdAt = testDocumentF.createdAt,
    updatedAt = testDocumentF.updatedAt
  )

  val testProjectScratchpad = ProjectScratchpad(
    userId = testUserC.id,
    projectId = testProjectB.id,
    version = testDocumentN.version,
    documentId = testDocumentN.id,
    createdAt = testDocumentN.createdAt,
    updatedAt = testDocumentN.updatedAt
  )

  val testProjectScratchpadD = ProjectScratchpad(
    userId = testUserE.id,
    projectId = testProjectB.id,
    version = testDocumentO.version,
    documentId = testDocumentO.id,
    createdAt = testDocumentO.createdAt,
    updatedAt = testDocumentO.updatedAt
  )
  //scratchPad that is not in the database, mysterious stranger
  val testProjectScratchpadX = ProjectScratchpad(
    userId = testUserA.id,
    projectId = testProjectC.id,
    documentId = testDocumentJ.id,
    document = Some(testDocumentJ),
    version = testDocumentJ.version,
    createdAt = testDocumentJ.createdAt,
    updatedAt = testDocumentJ.updatedAt
  )

  /* ---------------------- USER TOKENS ---------------------- */

  val testUserToken = UserToken(
    userId = UUID.fromString("8b6dc674-d1ae-11e5-9080-08626681851d"),
    token = "$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=",
    tokenType = "activation"
  //    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserTokenEmail = UserToken(
    userId = UUID.fromString("8b6dc674-d1ae-11e5-9080-08626681851d"),
    token = "$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=",
    tokenType = "activation"
  //    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testUserTokenInsert = UserToken(
    userId = UUID.fromString("4d01347e-c592-4e5f-b09f-dd281b3d9b87"),
    token = "$s0$100801$Im7kWa5XcOMHIilt7A==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7xjMLFYE=",
    tokenType = "activation"
  //    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )
  val testUserTokenDelete = UserToken(
    userId = UUID.fromString("8b6dc674-d1ae-11e5-9080-08626681851d"),
    token = "$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=",
    tokenType = "activation"
  //    createdAt = new DateTime(2014, 8, 7, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  /* ---------------------- WORDS ---------------------- */

  val testWordA = LinkWord(
    word = "bisexualpotato",
    lang = "eng"
  )

  val testWordB = LinkWord(
    word = "cielbleu",
    lang = "fr"
  )

  val testWordC = LinkWord(
    word = "omniscienttable",
    lang = "eng"
  )

  val testWordD = LinkWord(
    word = "alientea",
    lang = "eng"
  )

  val testWordE = LinkWord(
    word = "vinrouge",
    lang = "fr"
  )

  val testWordF = LinkWord(
    word = "интереснаяличность",
    lang = "ru"
  )

  /* ---------------------- REGISTRATION LINKS ---------------------- */

  /**
   * The registration links for students
   */
  val testLinkA = Link(
    link = testWordA.word,
    courseId = testCourseA.id,
    createdAt = new DateTime(2014, 6, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  val testLinkB = Link(
    link = testWordE.word,
    courseId = testCourseB.id,
    createdAt = new DateTime(2014, 6, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )

  //Not in the database
  val testLinkC = Link(
    link = testWordC.word,
    courseId = testCourseA.id,
    createdAt = new DateTime(2014, 6, 11, 14, 1, 19, 545, DateTimeZone.forID("-04"))
  )
  //user tokens
  var testUserTokenA = UserToken(
    userId = UUID.fromString("8b6dc674-d1ae-11e5-9080-08626681851d"),
    token = "$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=",
    tokenType = "activation"
  )

  var testUserTokenP = UserToken(
    userId = UUID.fromString("7c62a526-d1b0-11e5-9080-08626681851d"),
    token = "$s0$100801$Im7kWa5XcOMHIilt7A==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=",
    tokenType = "password_reset"
  )
}

