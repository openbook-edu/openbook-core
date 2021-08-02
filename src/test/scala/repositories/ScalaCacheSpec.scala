import ca.shiftfocus.krispii.core.repositories._
import org.scalatest.Matchers._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._

class ScalaCacheSpec
    extends TestEnvironment {
  val cacheRepository = new CacheRepository(scalaCacheConfig)

  "ScalaCacheSpec" should {
    inSequence {
      "ScalaCacheSpec [Course]" in {
        val cacheKey = "some_key"
        val testCourse = TestValues.testCourseA

        val putResult = cacheRepository.cacheCourse.putCache(cacheKey)(testCourse)
        val putResultDone = Await.result(putResult, Duration.Inf)
        val \/-(putCourse) = putResultDone

        val getResult = cacheRepository.cacheCourse.getCached(cacheKey)
        val getResultDone = Await.result(getResult, Duration.Inf)
        val \/-(course) = getResultDone

        course.name should be(testCourse.name)
      }
      "ScalaCacheSpec [String]" in {
        val cacheKey = "some_key_2"
        val testString = "some_string"

        val putResult = cacheRepository.cacheString.putCache(cacheKey)(testString)
        val putResultDone = Await.result(putResult, Duration.Inf)
        val \/-(putCourse) = putResultDone

        val getResult = cacheRepository.cacheString.getCached(cacheKey)
        val getResultDone = Await.result(getResult, Duration.Inf)
        val \/-(result) = getResultDone

        result should be(testString)
      }
      // Cache for String should be instantiated only once
      "ScalaCacheSpec multiple [String]" in {
        val cacheKey = "some_key_2"
        val testString = "some_string"

        cacheRepository.cacheString.putCache(cacheKey + "1")(testString)
        cacheRepository.cacheString.putCache(cacheKey + "2")(testString)
        cacheRepository.cacheString.putCache(cacheKey + "3")(testString)
        val putResult = cacheRepository.cacheString.putCache(cacheKey)(testString)
        val putResultDone = Await.result(putResult, Duration.Inf)
        val \/-(putCourse) = putResultDone

        val getResult = cacheRepository.cacheString.getCached(cacheKey)
        val getResultDone = Await.result(getResult, Duration.Inf)
        val \/-(result) = getResultDone

        result should be(testString)
      }
      "ScalaCacheSpec IndexedSeq[String]" in {
        val cacheKey = "some_key_3"
        val testSeqString = IndexedSeq("some_string")

        val putResult = cacheRepository.cacheSeqString.putCache(cacheKey)(testSeqString)
        val putResultDone = Await.result(putResult, Duration.Inf)
        val \/-(putCourse) = putResultDone

        val getResult = cacheRepository.cacheSeqString.getCached(cacheKey)
        val getResultDone = Await.result(getResult, Duration.Inf)
        val \/-(result) = getResultDone

        console_log(result)

        result should be(testSeqString)
      }
      "ScalaCacheSpec IndexedSeq[Task]" in {
        val cacheKey = "some_key_4"
        val testSeq = IndexedSeq(
          TestValues.testLongAnswerTaskA,
          TestValues.testShortAnswerTaskB
        )

        val putResult = cacheRepository.cacheSeqTask.putCache(cacheKey)(testSeq)
        val putResultDone = Await.result(putResult, Duration.Inf)
        val \/-(putCourse) = putResultDone

        val getResult = cacheRepository.cacheSeqTask.getCached(cacheKey)
        val getResultDone = Await.result(getResult, Duration.Inf)
        val \/-(result) = getResultDone

        console_log(result)

        result should be(testSeq)
      }
    }
  }
}