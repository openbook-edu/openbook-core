import ca.shiftfocus.krispii.core.models.CourseSchedule
import ca.shiftfocus.krispii.core.repositories.CourseScheduleRepositoryPostgres
import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scalaz.\/-

class CourseScheduleRepositorySpec
  extends TestEnvironment
{
  val courseScheduleRepository = new  CourseScheduleRepositoryPostgres

  // TODO - test all testcases
  "CourseScheduleRepository.list" should {
    inSequence {
      "list all schedules for a given class" in {
        (redisCache.get(_: String)) when(*) returns(Future.successful(None))
        (redisCache.put(_: String, _: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(Unit))

        val testCourse = TestValues.testCourseB

        val testCourseScheduleList = TreeMap[Int, CourseSchedule](
          0 -> TestValues.testCourseScheduleB,
          1 -> TestValues.testCourseScheduleC
        )

        val result = courseScheduleRepository.list(testCourse)
        val eitherCourseSchedules = Await.result(result, Duration.Inf)
        val \/-(courseSchedules) = eitherCourseSchedules

        courseSchedules.size should be(testCourseScheduleList.size)

        testCourseScheduleList.foreach {
          case (key, courseSchedule: CourseSchedule) => {
            courseSchedules(key).id should be(courseSchedule.id)
            courseSchedules(key).courseId should be(courseSchedule.courseId)
            courseSchedules(key).version should be(courseSchedule.version)
            courseSchedules(key).day should be(courseSchedule.day)
            courseSchedules(key).startTime should be(courseSchedule.startTime)
            courseSchedules(key).endTime should be(courseSchedule.endTime)
            courseSchedules(key).description should be(courseSchedule.description)
            courseSchedules(key).createdAt.toString should be(courseSchedule.createdAt.toString)
            courseSchedules(key).updatedAt.toString should be(courseSchedule.updatedAt.toString)
          }
        }
      }
    }
  }

  "CourseScheduleRepository.find" should {
    inSequence {
      "find a single entry by ID" in {
        (redisCache.get(_: String)) when(*) returns(Future.successful(None))
        (redisCache.put(_: String, _: Any, _: Option[Duration])) when(*, *, *) returns(Future.successful(Unit))

        val testCourseSchedule = TestValues.testCourseScheduleB

        val result = courseScheduleRepository.find(testCourseSchedule.id)
        val eitherCourseSchedule = Await.result(result, Duration.Inf)
        val \/-(courseSchedule) = eitherCourseSchedule

        courseSchedule.id should be(testCourseSchedule.id)
        courseSchedule.courseId should be(testCourseSchedule.courseId)
        courseSchedule.version should be(testCourseSchedule.version)
        courseSchedule.day should be(testCourseSchedule.day)
        courseSchedule.startTime should be(testCourseSchedule.startTime)
        courseSchedule.endTime should be(testCourseSchedule.endTime)
        courseSchedule.description should be(testCourseSchedule.description)
        courseSchedule.createdAt.toString should be(testCourseSchedule.createdAt.toString)
        courseSchedule.updatedAt.toString should be(testCourseSchedule.updatedAt.toString)
      }
    }
  }

  "CourseScheduleRepository.insert" should {
    inSequence {
      "create a new schedule" in {
        (redisCache.remove(_: String)) when(*) returns(Future.successful(Unit))

        val testCourseSchedule = TestValues.testCourseScheduleD

        val result = courseScheduleRepository.insert(testCourseSchedule)
        val eitherCourseSchedule = Await.result(result, Duration.Inf)
        val \/-(courseSchedule) = eitherCourseSchedule

        courseSchedule.id should be(testCourseSchedule.id)
        courseSchedule.courseId should be(testCourseSchedule.courseId)
        courseSchedule.version should be(1L)
        courseSchedule.day should be(testCourseSchedule.day)
        courseSchedule.startTime should be(testCourseSchedule.startTime)
        courseSchedule.endTime should be(testCourseSchedule.endTime)
        courseSchedule.description should be(testCourseSchedule.description)
      }
    }
  }

  "CourseScheduleRepository.update" should {
    inSequence {
      "update a schedule" in {
        (redisCache.remove(_: String)) when(*) returns(Future.successful(Unit))

        val testCourseSchedule = TestValues.testCourseScheduleA
        val testUpdatedCourseSchedule = testCourseSchedule.copy(
          courseId = TestValues.testCourseF.id,
          day = testCourseSchedule.day.plusDays(1),
          startTime = testCourseSchedule.startTime.plusHours(1),
          endTime = testCourseSchedule.endTime.plusHours(1),
          description = "new " + testCourseSchedule.description
        )

        val result = courseScheduleRepository.update(testUpdatedCourseSchedule)
        val eitherCourseSchedule = Await.result(result, Duration.Inf)
        val \/-(courseSchedule) = eitherCourseSchedule

        courseSchedule.id should be(testUpdatedCourseSchedule.id)
        courseSchedule.courseId should be(testUpdatedCourseSchedule.courseId)
        courseSchedule.version should be(testUpdatedCourseSchedule.version + 1)
        courseSchedule.day should be(testUpdatedCourseSchedule.day)
        courseSchedule.startTime should be(testUpdatedCourseSchedule.startTime)
        courseSchedule.endTime should be(testUpdatedCourseSchedule.endTime)
        courseSchedule.description should be(testUpdatedCourseSchedule.description)
        courseSchedule.updatedAt.toString should not be(testUpdatedCourseSchedule.updatedAt.toString)
        courseSchedule.createdAt.toString should be(testUpdatedCourseSchedule.createdAt.toString)
      }
    }
  }

  "CourseScheduleRepository.delete" should {
    inSequence {
      "delete a schedule" in {
        (redisCache.remove(_: String)) when(*) returns(Future.successful(Unit))

        val testCourseSchedule = TestValues.testCourseScheduleA

        val result = courseScheduleRepository.delete(testCourseSchedule)
        val eitherCourseSchedule = Await.result(result, Duration.Inf)
        val \/-(courseSchedule) = eitherCourseSchedule

        courseSchedule.id should be(testCourseSchedule.id)
        courseSchedule.courseId should be(testCourseSchedule.courseId)
        courseSchedule.version should be(testCourseSchedule.version)
        courseSchedule.day should be(testCourseSchedule.day)
        courseSchedule.startTime should be(testCourseSchedule.startTime)
        courseSchedule.endTime should be(testCourseSchedule.endTime)
        courseSchedule.description should be(testCourseSchedule.description)
        courseSchedule.createdAt.toString should be(testCourseSchedule.createdAt.toString)
        courseSchedule.updatedAt.toString should be(testCourseSchedule.updatedAt.toString)
      }
    }
  }
}
