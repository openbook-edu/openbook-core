import java.util.{Calendar, UUID}
import ca.shiftfocus.krispii.core.error.{RepositoryError, ServiceError}
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import org.scalatest._
import Matchers._
import ca.shiftfocus.krispii.core.models.GroupScheduleException
import ca.shiftfocus.krispii.core.models.group.Group
import ca.shiftfocus.krispii.core.models.user.User
import org.joda.time.DateTime

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scalaz.{-\/, \/-}

class ScheduleServiceSpec
    extends TestEnvironment(writeToDb = false) {
  val db = stub[DB]
  val mockConnection = stub[Connection]
  val authService = stub[AuthService]
  val omsService = stub[OmsService]
  val schoolService = stub[SchoolService]
  val projectService = stub[ProjectService]
  val groupScheduleRepository = stub[GroupScheduleRepository]
  val groupScheduleExceptionRepository = stub[GroupScheduleExceptionRepository]

  val scheduleService = new ScheduleServiceDefault(db, authService, omsService, schoolService, projectService, groupScheduleRepository, groupScheduleExceptionRepository) {
    override implicit def conn: Connection = mockConnection

    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "ScheduleService.updateSchedule" should {
    inSequence {
      "return ServiceError.OfflineLockFail if version doesn't match" in {
        val testSchedule = TestValues.testGroupScheduleA

        (groupScheduleRepository.find(_: UUID)(_: Connection)) when (testSchedule.id, *) returns (Future.successful(\/-(testSchedule)))

        val result = scheduleService.updateSchedule(
          testSchedule.id,
          99L,
          Some(testSchedule.groupId),
          Some(testSchedule.startDate),
          Some(testSchedule.endDate),
          Some(testSchedule.description)
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ScheduleService.createScheduleExceptions" should {
    inSequence {
      val testCourse = TestValues.testCourseA
      val testUsers: IndexedSeq[User] = IndexedSeq(TestValues.testUserA, TestValues.testUserB, TestValues.testUserC)
      val testWrongUsers: IndexedSeq[User] = IndexedSeq(TestValues.testUserA, TestValues.testUserB, TestValues.testUserC, TestValues.testUserD)
      val testUserIds: IndexedSeq[UUID] = testUsers.map(_.id)
      val testWrongUserIds: IndexedSeq[UUID] = testWrongUsers.map(_.id)
      val testGroupScheduleException = TestValues.testGroupScheduleExceptionA

      val scheduleException0 = testGroupScheduleException.copy(userId = testUserIds(0), groupId = testCourse.id)
      val scheduleException1 = testGroupScheduleException.copy(userId = testUserIds(1), groupId = testCourse.id)
      val scheduleException2 = testGroupScheduleException.copy(userId = testUserIds(2), groupId = testCourse.id)

      val expectedExceptions = Vector(scheduleException0, scheduleException1, scheduleException2)

      val expectedExceptionIds = expectedExceptions.map(_.id)

      val noResultsError = RepositoryError.NoResults("No group found with that Id")

      "return createdScheduleExceptions if all input is correct" in {
        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (authService.find(_: UUID)) when (testUserIds(0)) returns (Future.successful(\/-(testUsers(0)))) //A
        (authService.find(_: UUID)) when (testUserIds(1)) returns (Future.successful(\/-(testUsers(1)))) //B
        (authService.find(_: UUID)) when (testUserIds(2)) returns (Future.successful(\/-(testUsers(2)))) //C
        (schoolService.listStudents(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testUsers))) //ABC
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException0, *) returns (Future.successful(\/-(scheduleException0)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException1, *) returns (Future.successful(\/-(scheduleException1)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException2, *) returns (Future.successful(\/-(scheduleException2)))

        val result = scheduleService.createScheduleExceptions(
          testUserIds, //A, B, C
          testCourse.id,
          testGroupScheduleException.startDate,
          testGroupScheduleException.endDate,
          testGroupScheduleException.reason,
          expectedExceptionIds
        )
        Await.result(result, Duration.Inf) should be(\/-(expectedExceptions))
      }

      "return NoResults (No Course with that id) if invalid group id " in {
        val noResultsError = RepositoryError.NoResults("No group found with that Id")
        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(-\/(noResultsError)))
        (authService.find(_: UUID)) when (testUserIds(0)) returns (Future.successful(\/-(testUsers(0)))) //A
        (authService.find(_: UUID)) when (testUserIds(1)) returns (Future.successful(\/-(testUsers(1)))) //B
        (authService.find(_: UUID)) when (testUserIds(2)) returns (Future.successful(\/-(testUsers(2)))) //C
        (schoolService.listStudents(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testUsers))) //ABC
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException0, *) returns (Future.successful(\/-(scheduleException0)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException1, *) returns (Future.successful(\/-(scheduleException1)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException2, *) returns (Future.successful(\/-(scheduleException2)))

        val result = scheduleService.createScheduleExceptions(
          testUserIds, //A, B, C
          testCourse.id,
          testGroupScheduleException.startDate,
          testGroupScheduleException.endDate,
          testGroupScheduleException.reason,
          expectedExceptionIds
        )
        Await.result(result, Duration.Inf) should be(-\/(noResultsError))
      }

      "return NoResults (No User with that id) if invalid user id " in {
        val noResultsError = RepositoryError.NoResults("No user found with that Id")
        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (authService.find(_: UUID)) when (testUserIds(0)) returns (Future.successful(-\/(noResultsError))) //A
        (authService.find(_: UUID)) when (testUserIds(1)) returns (Future.successful(\/-(testUsers(1)))) //B
        (authService.find(_: UUID)) when (testUserIds(2)) returns (Future.successful(\/-(testUsers(2)))) //C
        (schoolService.listStudents(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testUsers))) //ABC
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException0, *) returns (Future.successful(\/-(scheduleException0)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException1, *) returns (Future.successful(\/-(scheduleException1)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException2, *) returns (Future.successful(\/-(scheduleException2)))

        val result = scheduleService.createScheduleExceptions(
          testUserIds, //A, B, C
          testCourse.id,
          testGroupScheduleException.startDate,
          testGroupScheduleException.endDate,
          testGroupScheduleException.reason,
          expectedExceptionIds
        )
        Await.result(result, Duration.Inf) should be(-\/(noResultsError))
      }

      "return NoResults(No Course with that ID upon listing students) " in {
        val noResultsError = RepositoryError.NoResults("No group found with that Id")
        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (authService.find(_: UUID)) when (testUserIds(0)) returns (Future.successful(\/-(testUsers(0)))) //A
        (authService.find(_: UUID)) when (testUserIds(1)) returns (Future.successful(\/-(testUsers(1)))) //B
        (authService.find(_: UUID)) when (testUserIds(2)) returns (Future.successful(\/-(testUsers(2)))) //C
        (schoolService.listStudents(_: UUID)) when (testCourse.id) returns (Future.successful(-\/(noResultsError))) //ABC
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException0, *) returns (Future.successful(\/-(scheduleException0)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException1, *) returns (Future.successful(\/-(scheduleException1)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException2, *) returns (Future.successful(\/-(scheduleException2)))

        val result = scheduleService.createScheduleExceptions(
          testUserIds, //A, B, C
          testCourse.id,
          testGroupScheduleException.startDate,
          testGroupScheduleException.endDate,
          testGroupScheduleException.reason,
          expectedExceptionIds
        )
        Await.result(result, Duration.Inf) should be(-\/(noResultsError))
      }

      "return PrimaryKeyFail if non unique keys" in {
        val primaryKeyFail = RepositoryError.PrimaryKeyConflict
        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (authService.find(_: UUID)) when (testUserIds(0)) returns (Future.successful(\/-(testUsers(0)))) //A users specified
        (authService.find(_: UUID)) when (testUserIds(1)) returns (Future.successful(\/-(testUsers(1)))) //B
        (authService.find(_: UUID)) when (testUserIds(2)) returns (Future.successful(\/-(testUsers(2)))) //C
        (schoolService.listStudents(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testUsers))) //ABC
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException0, *) returns (Future.successful(-\/(primaryKeyFail)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException1, *) returns (Future.successful(\/-(scheduleException1)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException2, *) returns (Future.successful(\/-(scheduleException2)))

        val result = scheduleService.createScheduleExceptions(
          testUserIds, //A, B, C
          testCourse.id,
          testGroupScheduleException.startDate,
          testGroupScheduleException.endDate,
          testGroupScheduleException.reason,
          expectedExceptionIds
        )
        Await.result(result, Duration.Inf) should be(-\/(primaryKeyFail))
      }

      "return BusinessLogicFail if invalid number of exception ids" in {
        val error = ServiceError.BusinessLogicFail("Invalid number of exception ids")
        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (authService.find(_: UUID)) when (testUserIds(0)) returns (Future.successful(\/-(testUsers(0)))) //A
        (authService.find(_: UUID)) when (testUserIds(1)) returns (Future.successful(\/-(testUsers(1)))) //B
        (authService.find(_: UUID)) when (testUserIds(2)) returns (Future.successful(\/-(testUsers(2)))) //C
        (schoolService.listStudents(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testUsers))) //ABC
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException0, *) returns (Future.successful(\/-(scheduleException0)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException1, *) returns (Future.successful(\/-(scheduleException1)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException2, *) returns (Future.successful(\/-(scheduleException2)))

        val result = scheduleService.createScheduleExceptions(
          testWrongUserIds,
          testCourse.id,
          testGroupScheduleException.startDate,
          testGroupScheduleException.endDate,
          testGroupScheduleException.reason,
          expectedExceptionIds
        )
        Await.result(result, Duration.Inf) should be(-\/(error))
      }

      "return BusinessLogicFail if users specified not in group" in {
        val error = ServiceError.BusinessLogicFail("User(s) specified not in group")
        val testWrongUsers: IndexedSeq[User] = IndexedSeq(TestValues.testUserB, TestValues.testUserC, TestValues.testUserD)
        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (authService.find(_: UUID)) when (testUserIds(0)) returns (Future.successful(\/-(testUsers(0)))) //A users specified
        (authService.find(_: UUID)) when (testUserIds(1)) returns (Future.successful(\/-(testUsers(1)))) //B
        (authService.find(_: UUID)) when (testUserIds(2)) returns (Future.successful(\/-(testUsers(2)))) //C
        (schoolService.listStudents(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testWrongUsers)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException0, *) returns (Future.successful(\/-(scheduleException0)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException1, *) returns (Future.successful(\/-(scheduleException1)))
        (groupScheduleExceptionRepository.insert(_: GroupScheduleException)(_: Connection)) when (scheduleException2, *) returns (Future.successful(\/-(scheduleException2)))

        val result = scheduleService.createScheduleExceptions(
          testUserIds,
          testCourse.id,
          testGroupScheduleException.startDate,
          testGroupScheduleException.endDate,
          testGroupScheduleException.reason,
          expectedExceptionIds
        )
        Await.result(result, Duration.Inf) should be(-\/(error))
      }
    }
  }

  "ScheduleService.deleteSchedule" should {
    inSequence {
      "return ServiceError.OfflineLockFail if version doesn't match" in {
        val testSchedule = TestValues.testGroupScheduleA

        (groupScheduleRepository.find(_: UUID)(_: Connection)) when (testSchedule.id, *) returns (Future.successful(\/-(testSchedule)))

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
        val testScheduleException = TestValues.testGroupScheduleExceptionA

        (groupScheduleExceptionRepository.find(_: UUID)(_: Connection)) when (testScheduleException.id, *) returns (Future.successful(\/-(testScheduleException)))

        val result = scheduleService.updateScheduleException(
          testScheduleException.id,
          99L,
          Some(testScheduleException.startDate),
          Some(testScheduleException.endDate),
          Some(testScheduleException.reason)
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
      }
    }
  }

  "ScheduleService.deleteScheduleException" should {
    inSequence {
      "return ServiceError.OfflineLockFail if version doesn't match" in {
        val testScheduleException = TestValues.testGroupScheduleExceptionA

        (groupScheduleExceptionRepository.find(_: UUID)(_: Connection)) when (testScheduleException.id, *) returns (Future.successful(\/-(testScheduleException)))

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
      "be TRUE if group has only schedules for today and now = startTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be TRUE if group has only schedules for today and now = endDate (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be TRUE if group has only schedules for today and startTime < now < endDate (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be FALSE if group is scheduled not for today and there are no scheduleExceptions for today too (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate.plusDays(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be false if group has only schedules for today and now > endDate (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate.plusDays(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if group has only schedules for today and now < startTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate.minusDays(1)
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
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }

      //ShceduleException -------------------
      "be TRUE if group has only scheduleExceptions for user for today and now = startTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector()
        val testScheduleExceptionList = Vector(
          TestValues.testGroupScheduleExceptionB,
          TestValues.testGroupScheduleExceptionC,
          TestValues.testGroupScheduleExceptionD
        )

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleExceptionList(2).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be TRUE if group has only scheduleExceptions for user for today and now = endDate (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector()
        val testScheduleExceptionList = Vector(
          TestValues.testGroupScheduleExceptionB,
          TestValues.testGroupScheduleExceptionC,
          TestValues.testGroupScheduleExceptionD
        )

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleExceptionList(2).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be TRUE if group has only scheduleExceptions for user for today startTime < now < endDate (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector()
        val testScheduleExceptionList = Vector(
          TestValues.testGroupScheduleExceptionB,
          TestValues.testGroupScheduleExceptionC,
          TestValues.testGroupScheduleExceptionD
        )

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleExceptionList(2).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be FALSE if group has only scheduleExceptions not for today and there are no schedules for today too (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector(
          TestValues.testGroupScheduleExceptionB,
          TestValues.testGroupScheduleExceptionC,
          TestValues.testGroupScheduleExceptionD
        )

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleExceptionList(2).startDate.plusDays(999)
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be false if group has scheduleExceptions for today and now > endDate (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector(
          TestValues.testGroupScheduleExceptionB,
          TestValues.testGroupScheduleExceptionC,
          TestValues.testGroupScheduleExceptionD
        )

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleExceptionList(2).startDate.plusDays(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if group has scheduleExceptions for today and now < startTime (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector(
          TestValues.testGroupScheduleExceptionB,
          TestValues.testGroupScheduleExceptionC,
          TestValues.testGroupScheduleExceptionD
        )

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleExceptionList(2).startDate.minusDays(1)
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if group has only scheduleExceptions for user and today = scheduleExceptions.startDate for another user and now = scheduleExceptions.startTime for another user (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector()
        val testScheduleExceptionList = Vector(
          TestValues.testGroupScheduleExceptionB,
          TestValues.testGroupScheduleExceptionC,
          TestValues.testGroupScheduleExceptionD
        )

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleExceptionList(0).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "be FALSE if there exists a blocking exception for the user and startTime < now < endDate" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector()
        val testScheduleExceptionList = Vector(
          TestValues.testGroupScheduleExceptionB,
          TestValues.testGroupScheduleExceptionC,
          TestValues.testGroupScheduleExceptionD.copy(block = true)
        )

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleExceptionList(2).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "return ServiceError.BadPermissions if user doesn't have the group (schedulingEnabled = TRUE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = true),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = true)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList.filter(_.id != testCourse.id))))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadPermissions("You must be a teacher or student of the relevant group to access this resource.")))
      }

      //schedulingEnabled = FALSE
      "be TRUE if user has group (schedulingEnabled = FALSE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = false),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = false)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(true))
      }
      "be FALSE if user has group but group is disabled (schedulingEnabled = FALSE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = false, schedulingEnabled = false),
          TestValues.testCourseB.copy(enabled = false, schedulingEnabled = false)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList)))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate
        )
        Await.result(result, Duration.Inf) should be(\/-(false))
      }
      "return ServiceError.BadPermissions if user doesn't have the group (schedulingEnabled = FALSE)" in {
        val testUser = TestValues.testUserE
        val testCourseList = Vector(
          TestValues.testCourseA.copy(enabled = true, schedulingEnabled = false),
          TestValues.testCourseB.copy(enabled = true, schedulingEnabled = false)
        )
        val testCourse = testCourseList(1)
        val testScheduleList = Vector(
          TestValues.testGroupScheduleB,
          TestValues.testGroupScheduleC
        )
        val testScheduleExceptionList = Vector()

        (schoolService.listCoursesByUser(_: UUID, _: Boolean)) when (testUser.id, false) returns (Future.successful(\/-(testCourseList.filter(_.id != testCourse.id))))

        (schoolService.findCourse(_: UUID)) when (testCourse.id) returns (Future.successful(\/-(testCourse)))
        (groupScheduleRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleList)))
        (groupScheduleExceptionRepository.list(_: Group)(_: Connection)) when (testCourse, *) returns (Future.successful(\/-(testScheduleExceptionList)))

        val result = scheduleService.isCourseScheduledForUser(
          testCourse,
          testUser.id,
          testScheduleList(0).startDate
        )
        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadPermissions("You must be a teacher or student of the relevant group to access this resource.")))
      }
    }
  }
}
