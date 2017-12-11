//import java.util.UUID
//
//import ca.shiftfocus.krispii.core.error.RepositoryError
//import ca.shiftfocus.krispii.core.models.CourseSchedule
//import ca.shiftfocus.krispii.core.repositories.CourseScheduleRepositoryPostgres
//import org.scalatest.Matchers._
//import org.scalatest._
//
//import scala.collection.immutable.TreeMap
//import scala.concurrent.duration.Duration
//import scala.concurrent.{ Await, Future }
//import scalaz.{ -\/, \/- }
//
//class CourseScheduleRepositorySpec
//    extends TestEnvironment {
//  val courseScheduleRepository = new CourseScheduleRepositoryPostgres
//
//  "CourseScheduleRepository.list" should {
//    inSequence {
//      "list all schedules for a given course" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseB
//
//        val testCourseScheduleList = TreeMap[Int, CourseSchedule](
//          0 -> TestValues.testCourseScheduleB,
//          1 -> TestValues.testCourseScheduleC
//        )
//
//        val result = courseScheduleRepository.list(testCourse)
//        val eitherCourseSchedules = Await.result(result, Duration.Inf)
//        val \/-(courseSchedules) = eitherCourseSchedules
//
//        courseSchedules.size should be(testCourseScheduleList.size)
//
//        testCourseScheduleList.foreach {
//          case (key, courseSchedule: CourseSchedule) => {
//            courseSchedules(key).id should be(courseSchedule.id)
//            courseSchedules(key).courseId should be(courseSchedule.courseId)
//            courseSchedules(key).version should be(courseSchedule.version)
//            courseSchedules(key).day should be(courseSchedule.day)
//            courseSchedules(key).startTime should be(courseSchedule.startTime)
//            courseSchedules(key).endTime should be(courseSchedule.endTime)
//            courseSchedules(key).description should be(courseSchedule.description)
//            courseSchedules(key).createdAt.toString should be(courseSchedule.createdAt.toString)
//            courseSchedules(key).updatedAt.toString should be(courseSchedule.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if course doesn't exist" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseC
//
//        val testCourseScheduleList = TreeMap[Int, CourseSchedule](
//          0 -> TestValues.testCourseScheduleB,
//          1 -> TestValues.testCourseScheduleC
//        )
//
//        val result = courseScheduleRepository.list(testCourse)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//    }
//  }
//
//  "CourseScheduleRepository.find" should {
//    inSequence {
//      "find a single entry by ID" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourseSchedule = TestValues.testCourseScheduleB
//
//        val result = courseScheduleRepository.find(testCourseSchedule.id)
//        val eitherCourseSchedule = Await.result(result, Duration.Inf)
//        val \/-(courseSchedule) = eitherCourseSchedule
//
//        courseSchedule.id should be(testCourseSchedule.id)
//        courseSchedule.courseId should be(testCourseSchedule.courseId)
//        courseSchedule.version should be(testCourseSchedule.version)
//        courseSchedule.day should be(testCourseSchedule.day)
//        courseSchedule.startTime should be(testCourseSchedule.startTime)
//        courseSchedule.endTime should be(testCourseSchedule.endTime)
//        courseSchedule.description should be(testCourseSchedule.description)
//        courseSchedule.createdAt.toString should be(testCourseSchedule.createdAt.toString)
//        courseSchedule.updatedAt.toString should be(testCourseSchedule.updatedAt.toString)
//      }
//      "return RepositoryError.NoResults if ID is wrong" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourseScheduleId = UUID.randomUUID()
//
//        val result = courseScheduleRepository.find(testCourseScheduleId)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type CourseSchedule")))
//      }
//    }
//  }
//
//  "CourseScheduleRepository.insert" should {
//    inSequence {
//      "create a new schedule" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseSchedule = TestValues.testCourseScheduleD
//
//        val result = courseScheduleRepository.insert(testCourseSchedule)
//        val eitherCourseSchedule = Await.result(result, Duration.Inf)
//        val \/-(courseSchedule) = eitherCourseSchedule
//
//        courseSchedule.id should be(testCourseSchedule.id)
//        courseSchedule.courseId should be(testCourseSchedule.courseId)
//        courseSchedule.version should be(1L)
//        courseSchedule.day should be(testCourseSchedule.day)
//        courseSchedule.startTime should be(testCourseSchedule.startTime)
//        courseSchedule.endTime should be(testCourseSchedule.endTime)
//        courseSchedule.description should be(testCourseSchedule.description)
//      }
//      "reutrn RepositoryError.PrimaryKeyConflict if courseSchedule already exists" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseSchedule = TestValues.testCourseScheduleA
//
//        val result = courseScheduleRepository.insert(testCourseSchedule)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
//      }
//    }
//  }
//
//  "CourseScheduleRepository.update" should {
//    inSequence {
//      "update a schedule" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseSchedule = TestValues.testCourseScheduleA
//        val testUpdatedCourseSchedule = testCourseSchedule.copy(
//          courseId = TestValues.testCourseF.id,
//          day = testCourseSchedule.day.plusDays(1),
//          startTime = testCourseSchedule.startTime.plusHours(1),
//          endTime = testCourseSchedule.endTime.plusHours(1),
//          description = "new " + testCourseSchedule.description
//        )
//
//        val result = courseScheduleRepository.update(testUpdatedCourseSchedule)
//        val eitherCourseSchedule = Await.result(result, Duration.Inf)
//        val \/-(courseSchedule) = eitherCourseSchedule
//
//        courseSchedule.id should be(testUpdatedCourseSchedule.id)
//        courseSchedule.courseId should be(testUpdatedCourseSchedule.courseId)
//        courseSchedule.version should be(testUpdatedCourseSchedule.version + 1)
//        courseSchedule.day should be(testUpdatedCourseSchedule.day)
//        courseSchedule.startTime should be(testUpdatedCourseSchedule.startTime)
//        courseSchedule.endTime should be(testUpdatedCourseSchedule.endTime)
//        courseSchedule.description should be(testUpdatedCourseSchedule.description)
//        courseSchedule.updatedAt.toString should not be (testUpdatedCourseSchedule.updatedAt.toString)
//        courseSchedule.createdAt.toString should be(testUpdatedCourseSchedule.createdAt.toString)
//      }
//      "reutrn RepositoryError.NoResults if version is wrong" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseSchedule = TestValues.testCourseScheduleA
//        val testUpdatedCourseSchedule = testCourseSchedule.copy(
//          version = testCourseSchedule.version + 1,
//          courseId = TestValues.testCourseF.id,
//          day = testCourseSchedule.day.plusDays(1),
//          startTime = testCourseSchedule.startTime.plusHours(1),
//          endTime = testCourseSchedule.endTime.plusHours(1),
//          description = "new " + testCourseSchedule.description
//        )
//
//        val result = courseScheduleRepository.update(testUpdatedCourseSchedule)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type CourseSchedule")))
//      }
//      "reutrn RepositoryError.NoResults if courseSchedule doesn't exist" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseSchedule = TestValues.testCourseScheduleD
//        val testUpdatedCourseSchedule = testCourseSchedule.copy(
//          courseId = TestValues.testCourseF.id,
//          day = testCourseSchedule.day.plusDays(1),
//          startTime = testCourseSchedule.startTime.plusHours(1),
//          endTime = testCourseSchedule.endTime.plusHours(1),
//          description = "new " + testCourseSchedule.description
//        )
//
//        val result = courseScheduleRepository.update(testUpdatedCourseSchedule)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type CourseSchedule")))
//      }
//    }
//  }
//
//  "CourseScheduleRepository.delete" should {
//    inSequence {
//      "delete a schedule" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseSchedule = TestValues.testCourseScheduleA
//
//        val result = courseScheduleRepository.delete(testCourseSchedule)
//        val eitherCourseSchedule = Await.result(result, Duration.Inf)
//        val \/-(courseSchedule) = eitherCourseSchedule
//
//        courseSchedule.id should be(testCourseSchedule.id)
//        courseSchedule.courseId should be(testCourseSchedule.courseId)
//        courseSchedule.version should be(testCourseSchedule.version)
//        courseSchedule.day should be(testCourseSchedule.day)
//        courseSchedule.startTime should be(testCourseSchedule.startTime)
//        courseSchedule.endTime should be(testCourseSchedule.endTime)
//        courseSchedule.description should be(testCourseSchedule.description)
//        courseSchedule.createdAt.toString should be(testCourseSchedule.createdAt.toString)
//        courseSchedule.updatedAt.toString should be(testCourseSchedule.updatedAt.toString)
//      }
//      "reutrn RepositoryError.NoResults if courseSchedule doesn't exist" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseSchedule = TestValues.testCourseScheduleD
//
//        val result = courseScheduleRepository.delete(testCourseSchedule)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type CourseSchedule")))
//      }
//      "reutrn RepositoryError.NoResults if courseSchedule version is wrong" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseSchedule = TestValues.testCourseScheduleA.copy(
//          version = 99L
//        )
//
//        val result = courseScheduleRepository.delete(testCourseSchedule)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type CourseSchedule")))
//      }
//    }
//  }
//}
