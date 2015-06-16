import java.util.UUID

import ca.shiftfocus.krispii.core.error.ServiceError
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models.Course
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
<<<<<<< HEAD
import org.joda.time.{ LocalTime, LocalDate }
=======
>>>>>>> aa4238e7e925ded712eb1a7f0142a3739a81bf9b

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
      // shcedule
      "be TRUE if course has only schedules for today and now = startTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).startTime
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be TRUE if course has only schedules for today and now = endTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).endTime
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be TRUE if course has only schedules for today and startTime < now < endTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).startTime.plusMinutes(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be FALSE if course is scheduled not for today and there are no scheduleExceptions for today too (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day.plusDays(1),
          testScheduleList(0).startTime
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be false if course has only schedules for today and now > endTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).endTime.plusMinutes(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if course has only schedules for today and now < startTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).startTime.minusMinutes(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if courses are disabled (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = false, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = false, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).startTime
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }

      // ShceduleException -------------------
      "be TRUE if course has only scheduleExceptions for user for today and now = startTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector()
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
          testScheduleExceptionList(2).day,
          testScheduleExceptionList(2).startTime
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be TRUE if course has only scheduleExceptions for user for today and now = endTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector()
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
          testScheduleExceptionList(2).day,
          testScheduleExceptionList(2).endTime
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be TRUE if course has only scheduleExceptions for user for today startTime < now < endTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector()
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
          testScheduleExceptionList(2).day,
          testScheduleExceptionList(2).startTime.plusMinutes(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be FALSE if course has only scheduleExceptions not for today and there are no schedules for today too (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
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
          testScheduleExceptionList(2).day.plusDays(999),
          testScheduleExceptionList(2).startTime
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be false if course has scheduleExceptions for today and now > endTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
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
          testScheduleExceptionList(2).day,
          testScheduleExceptionList(2).endTime.plusMinutes(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if course has scheduleExceptions for today and now < startTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
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
          testScheduleExceptionList(2).day,
          testScheduleExceptionList(2).startTime.minusMinutes(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if course has only scheduleExceptions for user and today = scheduleExceptions.day for another user and now = scheduleExceptions.startTime for another user (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector()
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
          testScheduleExceptionList(0).day,
          testScheduleExceptionList(0).startTime
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "return ServiceError.BadPermissions if user doesn't have the course (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList.filter(_.id != testCourse.id))))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).startTime
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadPermissions("You must be a teacher or student of the relevant course to access this resource.")))
      }

      // schedulingEnabled = FALSE
      "be TRUE if user has course (schedulingEnabled = FALSE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = false),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = false)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).startTime
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be FALSE if user has course but course is disabled (schedulingEnabled = FALSE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = false, schedulingEnabled = false),
          TestValues.testCourseB.copy(enabled = false, schedulingEnabled = false)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).startTime
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "return ServiceError.BadPermissions if user doesn't have the course (schedulingEnabled = FALSE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = false),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = false)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testCourseScheduleB,
          TestValues.testCourseScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID)) when (testUser.id) returns (Future.successful(\/-(testCourseList.filter(_.id != testCourse.id))))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (courseScheduleRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleList)))
        (courseScheduleExceptionRepository.list(_: Course)(_: Connection, _: ScalaCachePool)) when (testCourse, *, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).day,
          testScheduleList(0).startTime
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadPermissions("You must be a teacher or student of the relevant course to access this resource.")))
      }
    }
  }
}
