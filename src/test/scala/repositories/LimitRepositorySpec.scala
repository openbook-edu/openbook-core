import java.util.UUID

import ca.shiftfocus.krispii.core.repositories._
import org.joda.time.DateTime
import org.scalatest.Matchers._
import org.scalatest._

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz.{ -\/, \/- }

class LimitRepositorySpec extends TestEnvironment {
  val limitRepository = new LimitRepositoryPostgres
  //
  //  "LimitRepository.setCourseLimit" should {
  //    inSequence {
  //      "Set new course limit if it does not exist" in {
  //        val testTeacher = TestValues.testUserA
  //        val limit = 11
  //
  //        val result = limitRepository.setCourseLimit(testTeacher.id, limit)
  //        val eitherLimit = Await.result(result, Duration.Inf)
  //        val \/-(limitResult) = eitherLimit
  //
  //        limitResult should be(limit)
  //      }
  //    }
  //  }
  //
  //  "LimitRepository.setStudentLimit" should {
  //    inSequence {
  //      "Set new student limit if it does not exist" in {
  //        val testCourse = TestValues.testCourseA
  //        val limit = 40
  //
  //        val result = limitRepository.setStudentLimit(testCourse.id, limit)
  //        val eitherLimit = Await.result(result, Duration.Inf)
  //        val \/-(limitResult) = eitherLimit
  //
  //        limitResult should be(limit)
  //      }
  //    }
  //  }
  //
  //  "LimitRepository.setStorageLimit" should {
  //    inSequence {
  //      "Set new storage limit if it does not exist" in {
  //        val testUser = TestValues.testUserA
  //        val limit = 40
  //
  //        val result = limitRepository.setStorageLimit(testUser.id, limit.toFloat)
  //        val eitherLimit = Await.result(result, Duration.Inf)
  //        val \/-(limitResult) = eitherLimit
  //
  //        limitResult should be(limit)
  //      }
  //    }
  //  }

  "LimitRepository.setOrganizationDateLimit" should {
    inSequence {
      "Set new oraganization date limit if it does not exist" in {
        val limit = new DateTime(1492059509611L)

        val result = limitRepository.setOrganizationDateLimit(UUID.fromString("8c27b83b-af4f-4e7f-9ecf-8f6b5ec9bfb0"), limit)
        val eitherLimit = Await.result(result, Duration.Inf)
        val \/-(limitResult) = eitherLimit

        val result2 = limitRepository.setOrganizationDateLimit(UUID.fromString("8c27b83b-af4f-4e7f-9ecf-8f6b5ec9bfb0"), limit)
        val eitherLimit2 = Await.result(result2, Duration.Inf)
        val \/-(limitResult2) = eitherLimit2

        println(Console.GREEN + "limit = " + limit + Console.RESET)
        println(Console.GREEN + "limit ms = " + limit.getMillis + Console.RESET)
        println(Console.GREEN + "limitResult = " + limitResult + Console.RESET)
        println(Console.GREEN + "limitResult ms = " + limitResult.getMillis + Console.RESET)
        println(Console.GREEN + "limitResult2 = " + limitResult2 + Console.RESET)
        println(Console.GREEN + "limitResult2 ms = " + limitResult2.getMillis + Console.RESET)

        limitResult should be(limit)
      }
    }
  }
  //
  //  "LimitRepository.getStorageUsed" should {
  //    inSequence {
  //      "Get teacher storage used (Media work + media components), skip media component duplicates that share the same file on s3" in {
  //        val testTeacher = TestValues.testUserA
  //        // In GB
  //        val limit = (
  //          TestValues.testMediaWorkA.fileData.size.get +
  //          TestValues.testMediaWorkC.fileData.size.get +
  //          TestValues.testVideoComponentL.mediaData.size.get +
  //          TestValues.testAudioComponentM.mediaData.size.get
  //        ).toFloat / 1000 / 1000 / 1000
  //
  //        val result = limitRepository.getStorageUsed(testTeacher.id)
  //        val eitherLimit = Await.result(result, Duration.Inf)
  //        val \/-(limitResult) = eitherLimit
  //
  //        TestValues.testAudioComponentM.mediaData should be(TestValues.testAudioComponentN.mediaData)
  //        limitResult should be(limit)
  //      }
  //    }
  //  }
}
