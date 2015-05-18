import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.CourseScheduleException
import ca.shiftfocus.krispii.core.repositories._
import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scalaz.{-\/, \/-}

class CourseScheduleExceptionRepositorySpec
  extends TestEnvironment
{
  val userRepository = stub[UserRepository]
  val courseScheduleRepository = stub[CourseScheduleRepository]
  val courseScheduleExceptionRepository = new CourseScheduleExceptionRepositoryPostgres(userRepository, courseScheduleRepository)

  "CourseScheduleExceptionRepository.list" should {
    inSequence {
      "find all scheduling exceptions for one student in one course" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val testCourse = TestValues.testCourseB
        val testUser   = TestValues.testUserE

        val testCourseScheduleExceptionList = TreeMap[Int, CourseScheduleException](
          0 -> TestValues.testCourseScheduleExceptionC,
          1 -> TestValues.testCourseScheduleExceptionD
        )

        val result = courseScheduleExceptionRepository.list(testUser, testCourse)
        val eitherCourseSchedulesException = Await.result(result, Duration.Inf)
        val \/-(courseSchedulesExceptions) = eitherCourseSchedulesException

        courseSchedulesExceptions.size should be(testCourseScheduleExceptionList.size)

        testCourseScheduleExceptionList.foreach {
          case (key, courseScheduleException: CourseScheduleException) => {
            courseSchedulesExceptions(key).id should be(courseScheduleException.id)
            courseSchedulesExceptions(key).courseId should be(courseScheduleException.courseId)
            courseSchedulesExceptions(key).version should be(courseScheduleException.version)
            courseSchedulesExceptions(key).day should be(courseScheduleException.day)
            courseSchedulesExceptions(key).startTime should be(courseScheduleException.startTime)
            courseSchedulesExceptions(key).endTime should be(courseScheduleException.endTime)
            courseSchedulesExceptions(key).reason should be(courseScheduleException.reason)
            courseSchedulesExceptions(key).createdAt.toString should be(courseScheduleException.createdAt.toString)
            courseSchedulesExceptions(key).updatedAt.toString should be(courseScheduleException.updatedAt.toString)
          }
        }
      }
      "find all schedule exceptions for a given course" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val testCourse = TestValues.testCourseB

        val testCourseScheduleExceptionList = TreeMap[Int, CourseScheduleException](
          0 -> TestValues.testCourseScheduleExceptionB,
          1 -> TestValues.testCourseScheduleExceptionC,
          2 -> TestValues.testCourseScheduleExceptionD
        )

        val result = courseScheduleExceptionRepository.list(testCourse)
        val eitherCourseScheduleExceptions = Await.result(result, Duration.Inf)
        val \/-(courseSchedulesExceptions) = eitherCourseScheduleExceptions

        courseSchedulesExceptions.size should be(testCourseScheduleExceptionList.size)

        testCourseScheduleExceptionList.foreach {
          case (key, courseScheduleException: CourseScheduleException) => {
            courseSchedulesExceptions(key).id should be(courseScheduleException.id)
            courseSchedulesExceptions(key).courseId should be(courseScheduleException.courseId)
            courseSchedulesExceptions(key).version should be(courseScheduleException.version)
            courseSchedulesExceptions(key).day should be(courseScheduleException.day)
            courseSchedulesExceptions(key).startTime should be(courseScheduleException.startTime)
            courseSchedulesExceptions(key).endTime should be(courseScheduleException.endTime)
            courseSchedulesExceptions(key).reason should be(courseScheduleException.reason)
            courseSchedulesExceptions(key).createdAt.toString should be(courseScheduleException.createdAt.toString)
            courseSchedulesExceptions(key).updatedAt.toString should be(courseScheduleException.updatedAt.toString)
          }
        }
      }
    }
  }

  "CourseScheduleExceptionRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        (cache.getCached(_: String)) when(*) returns(Future.successful(-\/(RepositoryError.NoResults)))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(\/-(())))

        val testCourseScheduleException = TestValues.testCourseScheduleExceptionA

        val result = courseScheduleExceptionRepository.find(testCourseScheduleException.id)
        val eitherCourseScheduleException = Await.result(result, Duration.Inf)
        val \/-(courseSchedulesException) = eitherCourseScheduleException

        courseSchedulesException.id should be(testCourseScheduleException.id)
        courseSchedulesException.courseId should be(testCourseScheduleException.courseId)
        courseSchedulesException.version should be(testCourseScheduleException.version)
        courseSchedulesException.day should be(testCourseScheduleException.day)
        courseSchedulesException.startTime should be(testCourseScheduleException.startTime)
        courseSchedulesException.endTime should be(testCourseScheduleException.endTime)
        courseSchedulesException.reason should be(testCourseScheduleException.reason)
        courseSchedulesException.createdAt.toString should be(testCourseScheduleException.createdAt.toString)
        courseSchedulesException.updatedAt.toString should be(testCourseScheduleException.updatedAt.toString)
      }
    }
  }

  "CourseScheduleExceptionRepository.insert" should {
    inSequence {
      "create a new course schedule exception" in {
        (cache.removeCached(_: String)) when(*) returns(Future.successful(\/-( () )))

        val testCourseScheduleException = TestValues.testCourseScheduleExceptionE

        val result = courseScheduleExceptionRepository.insert(testCourseScheduleException)
        val eitherCourseScheduleException = Await.result(result, Duration.Inf)
        val \/-(courseSchedulesException) = eitherCourseScheduleException

        courseSchedulesException.id should be(testCourseScheduleException.id)
        courseSchedulesException.courseId should be(testCourseScheduleException.courseId)
        courseSchedulesException.version should be(1L)
        courseSchedulesException.day should be(testCourseScheduleException.day)
        courseSchedulesException.startTime should be(testCourseScheduleException.startTime)
        courseSchedulesException.endTime should be(testCourseScheduleException.endTime)
        courseSchedulesException.reason should be(testCourseScheduleException.reason)
      }
    }
  }

  "CourseScheduleExceptionRepository.update" should {
    inSequence {
      "update a course schedule exception" in {
        (cache.removeCached(_: String)) when(*) returns(Future.successful(\/-( () )))

        val testCourseScheduleException = TestValues.testCourseScheduleExceptionA
        val testUpdatedCourseScheduleException = testCourseScheduleException.copy(
          userId = TestValues.testUserG.id,
          courseId = TestValues.testCourseF.id,
          day = testCourseScheduleException.day.plusDays(1),
          startTime = testCourseScheduleException.startTime.plusHours(1),
          endTime = testCourseScheduleException.endTime.plusHours(1),
          reason = "new " + testCourseScheduleException.reason
        )

        val result = courseScheduleExceptionRepository.update(testUpdatedCourseScheduleException)
        val eitherCourseScheduleException = Await.result(result, Duration.Inf)
        val \/-(courseSchedulesException) = eitherCourseScheduleException

        courseSchedulesException.id should be(testUpdatedCourseScheduleException.id)
        courseSchedulesException.courseId should be(testUpdatedCourseScheduleException.courseId)
        courseSchedulesException.version should be(testUpdatedCourseScheduleException.version + 1)
        courseSchedulesException.day should be(testUpdatedCourseScheduleException.day)
        courseSchedulesException.startTime should be(testUpdatedCourseScheduleException.startTime)
        courseSchedulesException.endTime should be(testUpdatedCourseScheduleException.endTime)
        courseSchedulesException.reason should be(testUpdatedCourseScheduleException.reason)
        courseSchedulesException.createdAt.toString should be(testUpdatedCourseScheduleException.createdAt.toString)
        courseSchedulesException.updatedAt.toString should not be(testUpdatedCourseScheduleException.updatedAt.toString)
      }
    }
  }

  "CourseScheduleExceptionRepository.delete" should {
    inSequence {
      "delete a course schedule exception" in {
        (cache.removeCached(_: String)) when(*) returns(Future.successful(\/-( () )))

        val testCourseScheduleException = TestValues.testCourseScheduleExceptionA

        val result = courseScheduleExceptionRepository.delete(testCourseScheduleException)
        val eitherCourseScheduleException = Await.result(result, Duration.Inf)
        val \/-(courseSchedulesException) = eitherCourseScheduleException

        courseSchedulesException.id should be(testCourseScheduleException.id)
        courseSchedulesException.courseId should be(testCourseScheduleException.courseId)
        courseSchedulesException.version should be(1L)
        courseSchedulesException.day should be(testCourseScheduleException.day)
        courseSchedulesException.startTime should be(testCourseScheduleException.startTime)
        courseSchedulesException.endTime should be(testCourseScheduleException.endTime)
        courseSchedulesException.reason should be(testCourseScheduleException.reason)
        courseSchedulesException.createdAt.toString should be(testCourseScheduleException.createdAt.toString)
        courseSchedulesException.updatedAt.toString should be(testCourseScheduleException.updatedAt.toString)
      }
    }
  }
}