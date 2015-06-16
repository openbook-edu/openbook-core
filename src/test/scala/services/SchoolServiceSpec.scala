import java.util.UUID

import ca.shiftfocus.krispii.core.error.ServiceError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.services.{ AuthServiceDefault, SchoolServiceDefault, AuthService }
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import org.scalatest._
import Matchers._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz.{ -\/, \/- }

class SchoolServiceSpec
    extends TestEnvironment {
  val db = stub[DB]
  val mockConnection = stub[Connection]

  val userRepository = stub[UserRepository]
  val courseRepository = stub[CourseRepository]
  val chatRepository = stub[ChatRepository]
  val roleRepository = stub[RoleRepository]
  val sessionRepository = stub[SessionRepository]
  val authService = new AuthServiceDefault(db, cache, userRepository, roleRepository, sessionRepository)

  val schoolService = new SchoolServiceDefault(db, cache, authService, userRepository, courseRepository, chatRepository) {
    override implicit def conn: Connection = mockConnection

    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "SchoolService.createSchool" should {
    inSequence {
      "return ServiceError.BadPermissions if user is not a teacher" in {
        val testUser = TestValues.testUserA.copy(roles = IndexedSeq.empty[Role])
        val testRoles = IndexedSeq.empty[Role]
        val testCourse = TestValues.testCourseA

        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
        (roleRepository.list(_: User)(_: Connection, _: ScalaCachePool)) when (testUser, *, *) returns (Future.successful(\/-(testRoles)))

        val result = schoolService.createCourse(testUser.id, testCourse.name, testCourse.color, testCourse.slug)
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadPermissions("Tried to create a course for a user who isn't a teacher.")))
      }
    }
  }
}
