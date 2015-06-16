import java.util.UUID

import ca.shiftfocus.krispii.core.error.ServiceError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.Course
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import org.joda.time.{ LocalTime, LocalDate }

import org.scalatest._
import Matchers._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz.{ \/-, -\/ }

class ScheduleServiceSpec
    extends TestEnvironment(writeToDb = false) {
  val db = stub[DB]
  val mockConnection = stub[Connection]
  val authService = stub[AuthService]
  val schoolService = stub[SchoolService]
  val projectService = stub[ProjectService]
  val courseScheduleRepository = stub[CourseScheduleRepository]
  val courseScheduleExceptionRepository = stub[CourseScheduleExceptionRepository]

  val scheduleService = new ScheduleServiceDefault(db, cache, authService, schoolService, projectService, courseScheduleRepository, courseScheduleExceptionRepository) {
    override implicit def conn: Connection = mockConnection

    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "ScheduleService.updateSchedule" should {
    inSequence {
      "return ServiceError.OfflineLockFail if version doesn't match" in {
        val testSchedule = TestValues.testCourseScheduleA

        (courseScheduleRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testSchedule.id, *, *) returns (Future.successful(\/-(testSchedule)))

        val result = scheduleService.updateSchedule(
          testSchedule.id,
          99L,
          Some(testSchedule.courseId),
          Some(testSchedule.day),
          Some(testSchedule.startTime),
          Some(testSchedule.endTime),
          Some(testSchedule.description)
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ScheduleService.deleteSchedule" should {
    inSequence {
      "return ServiceError.OfflineLockFail if version doesn't match" in {
        val testSchedule = TestValues.testCourseScheduleA

        (courseScheduleRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testSchedule.id, *, *) returns (Future.successful(\/-(testSchedule)))

        val result = scheduleService.deleteSchedule(
          testSchedule.id,
          99L
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ScheduleService.updateScheduleException" should {
    inSequence {
      "return ServiceError.OfflineLockFail if version doesn't match" in {
        val testScheduleException = TestValues.testCourseScheduleExceptionA

        (courseScheduleExceptionRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testScheduleException.id, *, *) returns (Future.successful(\/-(testScheduleException)))

        val result = scheduleService.updateScheduleException(
          testScheduleException.id,
          99L,
          Some(testScheduleException.day),
          Some(testScheduleException.startTime),
          Some(testScheduleException.endTime),
          Some(testScheduleException.reason)
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ScheduleService.deleteScheduleException" should {
    inSequence {
      "return ServiceError.OfflineLockFail if version doesn't match" in {
        val testScheduleException = TestValues.testCourseScheduleExceptionA

        (courseScheduleExceptionRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testScheduleException.id, *, *) returns (Future.successful(\/-(testScheduleException)))

        val result = scheduleService.deleteScheduleException(
          testScheduleException.id,
          99L
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ScheduleService.isCourseScheduledForUser" should {
    inSequence {
      "be TRUE if course is scheduled" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA,
          TestValues.testCourseB
        )
        val testCourse = testCourseList(0)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector(
          TestValues.testCourseScheduleExceptionB,
          TestValues.testCourseScheduleExceptionC,
          TestValues.testCourseScheduleExceptionD
        )

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          new LocalDate(2015, 1, 15),
          new LocalTime(14, 1, 19)
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
    }
  }
}
