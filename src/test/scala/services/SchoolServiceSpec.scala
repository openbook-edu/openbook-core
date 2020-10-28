//import java.awt.Color
//import java.util.UUID
//
//import ca.shiftfocus.krispii.core.error.{ RepositoryError, ServiceError }
//import ca.shiftfocus.krispii.core.lib.ScalaCachePool
//import ca.shiftfocus.krispii.core.repositories._
//import ca.shiftfocus.krispii.core.models._
//import ca.shiftfocus.krispii.core.services.{ AuthServiceDefault, SchoolServiceDefault, AuthService }
//import ca.shiftfocus.krispii.core.services.datasource.DB
//import com.github.mauricio.async.db.Connection
//import org.joda.time.DateTime
//import org.scalatest._
//import Matchers._
//
//import scala.concurrent.duration.Duration
//import scala.concurrent.{ Await, Future }
//import scalaz.{ -\/, \/- }
//
//class SchoolServiceSpec
//    extends TestEnvironment(writeToDb = false) {
//  val db = stub[DB]
//  val mockConnection = stub[Connection]
//
//  val userRepository = stub[UserRepository]
//  val courseRepository = stub[CourseRepository]
//  val chatRepository = stub[ChatRepository]
//  val roleRepository = stub[RoleRepository]
//  val sessionRepository = stub[SessionRepository]
//  val authService = stub[AuthService]
//  val wordRepository = stub[WordRepository]
//  val linkRepository = stub[LinkRepository]
//  val limitRepository = stub[LimitRepository]
//  //  val authService = new AuthServiceDefault(db, cache, userRepository, roleRepository, sessionRepository)
//  val schoolService = new SchoolServiceDefault(db, cache, authService, userRepository, courseRepository, chatRepository, wordRepository, linkRepository, limitRepository) {
//    override implicit def conn: Connection = mockConnection
//
//    override def transactional[A](f: Connection => Future[A]): Future[A] = {
//      f(mockConnection)
//    }
//  }
//
//  "SchoolService.createCourse" should {
//    inSequence {
//      "return ServiceError.BadPermissions if user is not a teacher" in {
//        val testUser = TestValues.testUserA.copy(roles = IndexedSeq.empty[Role])
//        val testRoles = IndexedSeq.empty[Role]
//        val testCourse = TestValues.testCourseA
//
//        (authService.find(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testUser)))
//
//        val result = schoolService.createCourse(testUser.id, testCourse.name, testCourse.color, testCourse.slug)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadPermissions("Tried to create a group for a user who isn't a teacher.")))
//      }
//    }
//  }
//
//  "SchoolService.updateCourse" should {
//    inSequence {
//      "update group" in {
//        val testUser = TestValues.testUserA.copy(roles = IndexedSeq.empty[Role])
//        val testRoles = IndexedSeq(
//          TestValues.testRoleTeacher
//        )
//        val newTeacher = TestValues.testUserB
//        val testCourse = TestValues.testCourseA.copy(
//          ownerId = testUser.id,
//          enabled = false
//        )
//        val updatedCourse = testCourse.copy(
//          ownerId = newTeacher.id,
//          name = "new test group name",
//          color = new Color(78, 40, 23),
//          slug = "updated slug",
//          enabled = true,
//          schedulingEnabled = !testCourse.schedulingEnabled,
//          chatEnabled = !testCourse.chatEnabled
//        )
//
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//        (authService.find(_: UUID)) when (newTeacher.id) returns (Future.successful(\/-(newTeacher.copy(roles = testRoles))))
//        (courseRepository.update(_: Course)(_: Connection, _: ScalaCachePool)) when (updatedCourse, *, *) returns (Future.successful(\/-(updatedCourse)))
//
//        val result = schoolService.updateCourse(
//          updatedCourse.id,
//          updatedCourse.version,
//          Some(updatedCourse.ownerId),
//          Some(updatedCourse.name),
//          Some(updatedCourse.slug),
//          Some(updatedCourse.color),
//          Some(updatedCourse.enabled),
//          Some(updatedCourse.schedulingEnabled),
//          Some(updatedCourse.chatEnabled)
//        )
//
//        Await.result(result, Duration.Inf) should be(\/-(updatedCourse))
//      }
//      "update group if slug is empty and group is enabled" in {
//        val testUser = TestValues.testUserA.copy(roles = IndexedSeq.empty[Role])
//        val testRoles = IndexedSeq(
//          TestValues.testRoleTeacher
//        )
//        val newTeacher = TestValues.testUserB
//        val testCourse = TestValues.testCourseA.copy(
//          ownerId = testUser.id,
//          enabled = true
//        )
//        val updatedCourse = testCourse.copy(
//          ownerId = newTeacher.id,
//          name = "new test group name",
//          color = new Color(78, 40, 23),
//          enabled = true,
//          schedulingEnabled = !testCourse.schedulingEnabled,
//          chatEnabled = !testCourse.chatEnabled
//        )
//
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//        (authService.find(_: UUID)) when (newTeacher.id) returns (Future.successful(\/-(newTeacher.copy(roles = testRoles))))
//        (courseRepository.update(_: Course)(_: Connection, _: ScalaCachePool)) when (updatedCourse, *, *) returns (Future.successful(\/-(updatedCourse)))
//
//        val result = schoolService.updateCourse(
//          updatedCourse.id,
//          updatedCourse.version,
//          Some(updatedCourse.ownerId),
//          Some(updatedCourse.name),
//          None,
//          Some(updatedCourse.color),
//          Some(updatedCourse.enabled),
//          Some(updatedCourse.schedulingEnabled),
//          Some(updatedCourse.chatEnabled)
//        )
//
//        Await.result(result, Duration.Inf) should be(\/-(updatedCourse))
//      }
//      "return ServiceError.BusinessLogicFail if slug to update is not empty and group.enabled = TRUE" in {
//        val testUser = TestValues.testUserA.copy(roles = IndexedSeq.empty[Role])
//        val testRoles = IndexedSeq(
//          TestValues.testRoleTeacher
//        )
//        val newTeacher = TestValues.testUserB
//        val testCourse = TestValues.testCourseA.copy(
//          ownerId = testUser.id,
//          enabled = true
//        )
//        val updatedCourse = testCourse.copy(
//          ownerId = newTeacher.id,
//          name = "new test group name",
//          color = new Color(78, 40, 23),
//          slug = "updated slug",
//          enabled = true,
//          schedulingEnabled = !testCourse.schedulingEnabled,
//          chatEnabled = !testCourse.chatEnabled
//        )
//
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//        (authService.find(_: UUID)) when (newTeacher.id) returns (Future.successful(\/-(newTeacher.copy(roles = testRoles))))
//        (courseRepository.update(_: Course)(_: Connection, _: ScalaCachePool)) when (updatedCourse, *, *) returns (Future.successful(\/-(updatedCourse)))
//
//        val result = schoolService.updateCourse(
//          updatedCourse.id,
//          updatedCourse.version,
//          Some(updatedCourse.ownerId),
//          Some(updatedCourse.name),
//          Some(updatedCourse.slug),
//          Some(updatedCourse.color),
//          Some(updatedCourse.enabled),
//          Some(updatedCourse.schedulingEnabled),
//          Some(updatedCourse.chatEnabled)
//        )
//
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BusinessLogicFail("You can only change the slug for disabled courses.")))
//      }
//      "return ServiceError.BadPermissions if new group teacher doesn't have teacher role" in {
//        val testUser = TestValues.testUserA.copy(roles = IndexedSeq.empty[Role])
//        val newTeacher = TestValues.testUserB
//        val testCourse = TestValues.testCourseA.copy(
//          ownerId = testUser.id,
//          enabled = false
//        )
//        val updatedCourse = testCourse.copy(
//          ownerId = newTeacher.id,
//          name = "new test group name",
//          color = new Color(78, 40, 23),
//          slug = "updated slug",
//          enabled = true,
//          schedulingEnabled = !testCourse.schedulingEnabled,
//          chatEnabled = !testCourse.chatEnabled
//        )
//
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//        (authService.find(_: UUID)) when (newTeacher.id) returns (Future.successful(\/-(newTeacher)))
//        (courseRepository.update(_: Course)(_: Connection, _: ScalaCachePool)) when (updatedCourse, *, *) returns (Future.successful(\/-(updatedCourse)))
//
//        val result = schoolService.updateCourse(
//          updatedCourse.id,
//          updatedCourse.version,
//          Some(updatedCourse.ownerId),
//          Some(updatedCourse.name),
//          Some(updatedCourse.slug),
//          Some(updatedCourse.color),
//          Some(updatedCourse.enabled),
//          Some(updatedCourse.schedulingEnabled),
//          Some(updatedCourse.chatEnabled)
//        )
//
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadPermissions("Tried to update a group for a user who isn't a teacher.")))
//      }
//    }
//  }
//
//  "SchoolService.deleteCourse" should {
//    inSequence {
//      "return ServiceError.OfflineLockFail if versions don't match" in {
//        val testCourse = TestValues.testCourseA
//
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful((\/-(testCourse))))
//
//        val result = schoolService.deleteCourse(testCourse.id, testCourse.version + 1)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
//      }
//    }
//  }
//
//  "SchoolService.findUserForTeacher" should {
//    inSequence {
//      "return RepositoryError.NoResults if user is not in the teacher's class" in {
//        val testStudent = TestValues.testUserA
//        val testTeacher = TestValues.testUserB
//        val testCourses = IndexedSeq()
//
//        (authService.find(_: UUID)) when (testStudent.id) returns (Future.successful(\/-(testStudent)))
//        (authService.find(_: UUID)) when (testTeacher.id) returns (Future.successful(\/-(testTeacher)))
//        (courseRepository.list(_: User, _: Boolean)(_: Connection, _: ScalaCachePool)) when (testStudent, false, *, *) returns (Future.successful(\/-(testCourses)))
//
//        val result = schoolService.findUserForTeacher(testStudent.id, testTeacher.id)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults(s"User ${testStudent.id.toString} is not in any courses wiht teacher ${testTeacher.id.toString}")))
//      }
//    }
//  }
//
//  "SchoolService.listChats" should {
//    inSequence {
//      "return ServiceError.BadInput if num is negative" in {
//        val testCourse = TestValues.testCourseA
//
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//
//        val result = schoolService.listChats(testCourse.id, -1L, 1L)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("num, and offset parameters must be positive long integers")))
//      }
//      "return ServiceError.BadInput if offset is negative" in {
//        val testCourse = TestValues.testCourseA
//
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//
//        val result = schoolService.listChats(testCourse.id, 1L, -1L)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("num, and offset parameters must be positive long integers")))
//      }
//      "return ServiceError.BadInput if num and offset are negative" in {
//        val testCourse = TestValues.testCourseA
//
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//
//        val result = schoolService.listChats(testCourse.id, -1L, -1L)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("num, and offset parameters must be positive long integers")))
//      }
//
//      // FOR USER
//      "return ServiceError.BadInput if num is negative (FOR USER)" in {
//        val testUser = TestValues.testUserA
//        val testCourse = TestValues.testCourseA
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//
//        val result = schoolService.listChats(testCourse.id, testUser.id, -1L, 1L)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("num, and offset parameters must be positive long integers")))
//      }
//      "return ServiceError.BadInput if offset is negative (FOR USER)" in {
//        val testUser = TestValues.testUserA
//        val testCourse = TestValues.testCourseA
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//
//        val result = schoolService.listChats(testCourse.id, testUser.id, 1L, -1L)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("num, and offset parameters must be positive long integers")))
//      }
//      "return ServiceError.BadInput if num and offset are negative (FOR USER)" in {
//        val testUser = TestValues.testUserA
//        val testCourse = TestValues.testCourseA
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//
//        val result = schoolService.listChats(testCourse.id, testUser.id, -1L, -1L)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("num, and offset parameters must be positive long integers")))
//      }
//    }
//  }
//
//  "SchoolService.insertChat" should {
//    inSequence {
//      "insert new chat if user is a teacher" in {
//        val testUser = TestValues.testUserA
//        val testCourse = TestValues.testCourseA.copy(ownerId = testUser.id)
//        val studentList = IndexedSeq()
//        val testChat = TestValues.testChatA
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//        (userRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(studentList)))
//        (chatRepository.insert(_: Chat)(_: Connection)) when (*, *) returns (Future.successful(\/-(testChat)))
//
//        val result = schoolService.insertChat(testCourse.id, testUser.id, testChat.message)
//        Await.result(result, Duration.Inf) should be(\/-(testChat))
//      }
//      "insert new chat if user is a student of the group" in {
//        val testUser = TestValues.testUserA
//        val testCourse = TestValues.testCourseA.copy(ownerId = UUID.randomUUID())
//        val studentList = IndexedSeq(
//          testUser
//        )
//        val testChat = TestValues.testChatA
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//        (userRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(studentList)))
//        (chatRepository.insert(_: Chat)(_: Connection)) when (*, *) returns (Future.successful(\/-(testChat)))
//
//        val result = schoolService.insertChat(testCourse.id, testUser.id, testChat.message)
//        Await.result(result, Duration.Inf) should be(\/-(testChat))
//      }
//      "return ServiceError.BadPermissions if user is not a teacher and doesn't attend the group" in {
//        val testUser = TestValues.testUserA
//        val testCourse = TestValues.testCourseA.copy(ownerId = UUID.randomUUID())
//        val studentList = IndexedSeq()
//        val testChat = TestValues.testChatA
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (courseRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testCourse.id, *, *) returns (Future.successful(\/-(testCourse)))
//        (userRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(studentList)))
//        (chatRepository.insert(_: Chat)(_: Connection)) when (*, *) returns (Future.successful(\/-(testChat)))
//
//        val result = schoolService.insertChat(testCourse.id, testUser.id, testChat.message)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadPermissions("You must be a member of a group to chat in it.")))
//      }
//    }
//  }
//  "SchoolService.createLink" should {
//    inSequence {
//      "Create a valid link if it wasn't created before" in {
//        val testWord = TestValues.testWordC
//        val testLink = TestValues.testLinkC
//
//        (wordRepository.get(_: String)(_: Connection)) when (*, *) returns (Future.successful(\/-(testWord)))
//        (linkRepository.create(_: Link)(_: Connection)) when (*, *) returns (Future.successful(\/-(testLink)))
//
//        val result = schoolService.createLink(testWord.lang, testLink.groupId)
//        Await.result(result, Duration.Inf) should be(\/-(testLink))
//      }
//    }
//  }
//}
