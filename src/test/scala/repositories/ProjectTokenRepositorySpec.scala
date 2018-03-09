import ca.shiftfocus.krispii.core.repositories._
import org.scalatest.Matchers._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._

//class ProjectTokenRepositorySpec
//    extends TestEnvironment {
//  val projectTokenRepository = new ProjectTokenRepositoryPostgres()
//
//  "ProjectTokenRepository.delete" should {
//    inSequence {
//      "delete token" in {
//        val testToken = TestValues.testProjectTokenA
//
//        val result = projectTokenRepository.delete(testToken)
//        val eitherProjectToken = Await.result(result, Duration.Inf)
//        val \/-(token) = eitherProjectToken
//
//        token.projectId should be(testToken.projectId)
//        token.email should be(testToken.email)
//        token.token should be(testToken.token)
//        token.createdAt.toString should be(testToken.createdAt.toString)
//      }
//    }
//  }
//}