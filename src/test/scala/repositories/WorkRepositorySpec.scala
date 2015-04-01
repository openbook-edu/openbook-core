import ca.shiftfocus.krispii.core.models.work.Work
import ca.shiftfocus.krispii.core.repositories.WorkRepositoryPostgres

import org.scalatest._
import Matchers._
import scala.collection.immutable.TreeMap
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._

class WorkRepositorySpec
  extends TestEnvironment {
  val workRepository = new WorkRepositoryPostgres

  "WorkRepository.list" should {
    inSequence {
      "list the latest revision of work for each task in a project" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA
        )

        val result = workRepository.list(testUser, testProject)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)
      }
      "list all revisions of a specific work for a user" in {
        val testUser = TestValues.testUserC
        val testTask = TestValues.testLongAnswerTaskA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA
        )

        val result = workRepository.list(testUser, testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)
      }
      "list all revisions of a specific work for all users" in {
        val testTask = TestValues.testLongAnswerTaskA

        val testWorkList = TreeMap[Int, Work](
          0 -> TestValues.testLongAnswerWorkA
        )

        val result = workRepository.list(testTask)
        val eitherWorks = Await.result(result, Duration.Inf)
        val \/-(works) = eitherWorks

        works.size should be(testWorkList.size)
      }
    }
  }
}
