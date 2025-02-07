//import ca.shiftfocus.krispii.core.error.RepositoryError
//import ca.shiftfocus.krispii.core.models.CourseScheduleException
//import ca.shiftfocus.krispii.core.repositories._
//import org.scalatest.Matchers._
//import org.scalatest._
//
//import scala.collection.immutable.TreeMap
//import scala.concurrent.duration.Duration
//import scala.concurrent.{ Await, Future }
//import scalaz.{ -\/, \/- }
//
//class CourseScheduleExceptionRepositorySpec
//    extends TestEnvironment {
//  val userRepository = stub[UserRepository]
//  val courseScheduleRepository = stub[CourseScheduleRepository]
//  val courseScheduleExceptionRepository = new GroupScheduleExceptionRepositoryPostgres(userRepository, courseScheduleRepository)
//
//  "groupScheduleExceptionRepository.list" should {
//    inSequence {
//      "find all scheduling exceptions for one student in one group" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseB
//        val testUser = TestValues.testUserE
//
//        val testCourseScheduleExceptionList = TreeMap[Int, CourseScheduleException](
//          0 -> TestValues.testCourseScheduleExceptionC,
//          1 -> TestValues.testCourseScheduleExceptionD
//        )
//
//        val result = courseScheduleExceptionRepository.list(testUser, testCourse)
//        val eitherCourseSchedulesException = Await.result(result, Duration.Inf)
//        val \/-(courseSchedulesExceptions) = eitherCourseSchedulesException
//
//        courseSchedulesExceptions.size should be(testCourseScheduleExceptionList.size)
//
//        testCourseScheduleExceptionList.foreach {
//          case (key, courseScheduleException: CourseScheduleException) => {
//            courseSchedulesExceptions(key).id should be(courseScheduleException.id)
//            courseSchedulesExceptions(key).groupId should be(courseScheduleException.groupId)
//            courseSchedulesExceptions(key).version should be(courseScheduleException.version)
//            courseSchedulesExceptions(key).startDay should be(courseScheduleException.startDay)
//            courseSchedulesExceptions(key).startTime should be(courseScheduleException.startTime)
//            courseSchedulesExceptions(key).endTime should be(courseScheduleException.endTime)
//            courseSchedulesExceptions(key).reason should be(courseScheduleException.reason)
//            courseSchedulesExceptions(key).createdAt.toString should be(courseScheduleException.createdAt.toString)
//            courseSchedulesExceptions(key).updatedAt.toString should be(courseScheduleException.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if Course doesn't exist (for one student)" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseC
//        val testUser = TestValues.testUserE
//
//        val testCourseScheduleExceptionList = TreeMap[Int, CourseScheduleException](
//          0 -> TestValues.testCourseScheduleExceptionC,
//          1 -> TestValues.testCourseScheduleExceptionD
//        )
//
//        val result = courseScheduleExceptionRepository.list(testUser, testCourse)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//      "return empty Vector() if User doesn't exist" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseB
//        val testUser = TestValues.testUserD
//
//        val testCourseScheduleExceptionList = TreeMap[Int, CourseScheduleException](
//          0 -> TestValues.testCourseScheduleExceptionC,
//          1 -> TestValues.testCourseScheduleExceptionD
//        )
//
//        val result = courseScheduleExceptionRepository.list(testUser, testCourse)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//      "find all schedule exceptions for a given group" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseB
//
//        val testCourseScheduleExceptionList = TreeMap[Int, CourseScheduleException](
//          0 -> TestValues.testCourseScheduleExceptionB,
//          1 -> TestValues.testCourseScheduleExceptionC,
//          2 -> TestValues.testCourseScheduleExceptionD
//        )
//
//        val result = courseScheduleExceptionRepository.list(testCourse)
//        val eitherCourseScheduleExceptions = Await.result(result, Duration.Inf)
//        val \/-(courseSchedulesExceptions) = eitherCourseScheduleExceptions
//
//        courseSchedulesExceptions.size should be(testCourseScheduleExceptionList.size)
//
//        testCourseScheduleExceptionList.foreach {
//          case (key, courseScheduleException: CourseScheduleException) => {
//            courseSchedulesExceptions(key).id should be(courseScheduleException.id)
//            courseSchedulesExceptions(key).groupId should be(courseScheduleException.groupId)
//            courseSchedulesExceptions(key).version should be(courseScheduleException.version)
//            courseSchedulesExceptions(key).startDay should be(courseScheduleException.startDay)
//            courseSchedulesExceptions(key).startTime should be(courseScheduleException.startTime)
//            courseSchedulesExceptions(key).endTime should be(courseScheduleException.endTime)
//            courseSchedulesExceptions(key).reason should be(courseScheduleException.reason)
//            courseSchedulesExceptions(key).createdAt.toString should be(courseScheduleException.createdAt.toString)
//            courseSchedulesExceptions(key).updatedAt.toString should be(courseScheduleException.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if Course doesn't exist" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourse = TestValues.testCourseC
//
//        val testCourseScheduleExceptionList = TreeMap[Int, CourseScheduleException](
//          0 -> TestValues.testCourseScheduleExceptionB,
//          1 -> TestValues.testCourseScheduleExceptionC,
//          2 -> TestValues.testCourseScheduleExceptionD
//        )
//
//        val result = courseScheduleExceptionRepository.list(testCourse)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//    }
//  }
//
//  "groupScheduleExceptionRepository.find" should {
//    inSequence {
//      "find a single entry by ID" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourseScheduleException = TestValues.testCourseScheduleExceptionA
//
//        val result = courseScheduleExceptionRepository.find(testCourseScheduleException.id)
//        val eitherCourseScheduleException = Await.result(result, Duration.Inf)
//        val \/-(courseSchedulesException) = eitherCourseScheduleException
//
//        courseSchedulesException.id should be(testCourseScheduleException.id)
//        courseSchedulesException.groupId should be(testCourseScheduleException.groupId)
//        courseSchedulesException.version should be(testCourseScheduleException.version)
//        courseSchedulesException.startDay should be(testCourseScheduleException.startDay)
//        courseSchedulesException.startTime should be(testCourseScheduleException.startTime)
//        courseSchedulesException.endTime should be(testCourseScheduleException.endTime)
//        courseSchedulesException.reason should be(testCourseScheduleException.reason)
//        courseSchedulesException.createdAt.toString should be(testCourseScheduleException.createdAt.toString)
//        courseSchedulesException.updatedAt.toString should be(testCourseScheduleException.updatedAt.toString)
//      }
//      "return RepositoryError.NoResults if CourseScheduleException doesn't exist" in {
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val testCourseScheduleException = TestValues.testCourseScheduleExceptionE
//
//        val result = courseScheduleExceptionRepository.find(testCourseScheduleException.id)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type CourseScheduleException")))
//      }
//    }
//  }
//
//  "groupScheduleExceptionRepository.insert" should {
//    inSequence {
//      "create a new group schedule exception" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseScheduleException = TestValues.testCourseScheduleExceptionE
//
//        val result = courseScheduleExceptionRepository.insert(testCourseScheduleException)
//        val eitherCourseScheduleException = Await.result(result, Duration.Inf)
//        val \/-(courseSchedulesException) = eitherCourseScheduleException
//
//        courseSchedulesException.id should be(testCourseScheduleException.id)
//        courseSchedulesException.groupId should be(testCourseScheduleException.groupId)
//        courseSchedulesException.version should be(1L)
//        courseSchedulesException.startDay should be(testCourseScheduleException.startDay)
//        courseSchedulesException.startTime should be(testCourseScheduleException.startTime)
//        courseSchedulesException.endTime should be(testCourseScheduleException.endTime)
//        courseSchedulesException.reason should be(testCourseScheduleException.reason)
//      }
//    }
//  }
//
//  "groupScheduleExceptionRepository.update" should {
//    inSequence {
//      "update a group schedule exception" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseScheduleException = TestValues.testCourseScheduleExceptionA
//        val testUpdatedCourseScheduleException = testCourseScheduleException.copy(
//          userId = TestValues.testUserG.id,
//          groupId = TestValues.testCourseF.id,
//          startDay = testCourseScheduleException.startDay.plusDays(1),
//          startTime = testCourseScheduleException.startTime.plusHours(1),
//          endTime = testCourseScheduleException.endTime.plusHours(1),
//          reason = "new " + testCourseScheduleException.reason
//        )
//
//        val result = courseScheduleExceptionRepository.update(testUpdatedCourseScheduleException)
//        val eitherCourseScheduleException = Await.result(result, Duration.Inf)
//        val \/-(courseSchedulesException) = eitherCourseScheduleException
//
//        courseSchedulesException.id should be(testUpdatedCourseScheduleException.id)
//        courseSchedulesException.groupId should be(testUpdatedCourseScheduleException.groupId)
//        courseSchedulesException.version should be(testUpdatedCourseScheduleException.version + 1)
//        courseSchedulesException.startDay should be(testUpdatedCourseScheduleException.startDay)
//        courseSchedulesException.startTime should be(testUpdatedCourseScheduleException.startTime)
//        courseSchedulesException.endTime should be(testUpdatedCourseScheduleException.endTime)
//        courseSchedulesException.reason should be(testUpdatedCourseScheduleException.reason)
//        courseSchedulesException.createdAt.toString should be(testUpdatedCourseScheduleException.createdAt.toString)
//        courseSchedulesException.updatedAt.toString should not be (testUpdatedCourseScheduleException.updatedAt.toString)
//      }
//      "return RepositoryError.NoResults if group schedule exception doesn't exist" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseScheduleException = TestValues.testCourseScheduleExceptionE
//        val testUpdatedCourseScheduleException = testCourseScheduleException.copy(
//          userId = TestValues.testUserG.id,
//          groupId = TestValues.testCourseF.id,
//          startDay = testCourseScheduleException.startDay.plusDays(1),
//          startTime = testCourseScheduleException.startTime.plusHours(1),
//          endTime = testCourseScheduleException.endTime.plusHours(1),
//          reason = "new " + testCourseScheduleException.reason
//        )
//
//        val result = courseScheduleExceptionRepository.update(testUpdatedCourseScheduleException)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type CourseScheduleException")))
//      }
//    }
//  }
//
//  "groupScheduleExceptionRepository.delete" should {
//    inSequence {
//      "delete a group schedule exception" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseScheduleException = TestValues.testCourseScheduleExceptionA
//
//        val result = courseScheduleExceptionRepository.delete(testCourseScheduleException)
//        val eitherCourseScheduleException = Await.result(result, Duration.Inf)
//        val \/-(courseSchedulesException) = eitherCourseScheduleException
//
//        courseSchedulesException.id should be(testCourseScheduleException.id)
//        courseSchedulesException.groupId should be(testCourseScheduleException.groupId)
//        courseSchedulesException.version should be(1L)
//        courseSchedulesException.startDay should be(testCourseScheduleException.startDay)
//        courseSchedulesException.startTime should be(testCourseScheduleException.startTime)
//        courseSchedulesException.endTime should be(testCourseScheduleException.endTime)
//        courseSchedulesException.reason should be(testCourseScheduleException.reason)
//        courseSchedulesException.createdAt.toString should be(testCourseScheduleException.createdAt.toString)
//        courseSchedulesException.updatedAt.toString should be(testCourseScheduleException.updatedAt.toString)
//      }
//      "return RepositoryError.NoResults if version is wrong" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseScheduleException = TestValues.testCourseScheduleExceptionA.copy(
//          version = 99L
//        )
//
//        val result = courseScheduleExceptionRepository.delete(testCourseScheduleException)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type CourseScheduleException")))
//      }
//      "return RepositoryError.NoResults if group schedule exception doesn't exist" in {
//        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))
//
//        val testCourseScheduleException = TestValues.testCourseScheduleExceptionE
//
//        val result = courseScheduleExceptionRepository.delete(testCourseScheduleException)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type CourseScheduleException")))
//      }
//    }
//  }
//}