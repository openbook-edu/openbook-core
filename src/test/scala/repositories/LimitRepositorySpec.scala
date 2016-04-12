import ca.shiftfocus.krispii.core.repositories._
import org.scalatest.Matchers._
import org.scalatest._
import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz.{ -\/, \/- }

class LimitRepositorySpec extends TestEnvironment {
  val limitRepository = new LimitRepositoryPostgres

  "LimitRepository.setCourseLimit" should {
    inSequence {
      "Set new course limit if it does not exist" in {
        val testTeacher = TestValues.testUserA
        val limit = 11

        val result = limitRepository.setCourseLimit(testTeacher.id, limit)
        val eitherLimit = Await.result(result, Duration.Inf)
        val \/-(limitResult) = eitherLimit

        limitResult should be(limit)
      }
    }
  }

  "LimitRepository.setStudentLimit" should {
    inSequence {
      "Set new student limit if it does not exist" in {
        val testCourse = TestValues.testCourseA
        val limit = 40

        val result = limitRepository.setStudentLimit(testCourse.id, limit)
        val eitherLimit = Await.result(result, Duration.Inf)
        val \/-(limitResult) = eitherLimit

        limitResult should be(limit)
      }
    }
  }
}
