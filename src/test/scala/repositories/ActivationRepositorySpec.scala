import java.util.UUID

import org.scalatest.Matchers._

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.repositories.ActivationRepositoryPostgres

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.Duration
import scalaz.{ -\/, \/- }

/**
 * Created by ryanez on 11/02/16.
 */
class ActivationRepositorySpec extends TestEnvironment {
  val activationRepository = new ActivationRepositoryPostgres

  "Find one activation by the user id" in {
    (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
    (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
    val result = activationRepository.find(UUID.fromString("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

    val testToken = TestValues.testUserToken

    val eitherToken = Await.result(result, Duration.Inf)

    val \/-(token) = eitherToken

    token.userId should be(testToken.userId)
    token.token should be(testToken.token)
    token.createdAt should be(testToken.createdAt)

  }

  "Find one activation by the user email" in {
    (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
    (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
    val result = activationRepository.find("rafael@krispii.com")

    val testToken = TestValues.testUserTokenEmail

    val eitherToken = Await.result(result, Duration.Inf)

    val \/-(token) = eitherToken

    token.userId should be(testToken.userId)
    token.token should be(testToken.token)
    token.createdAt should be(testToken.createdAt)
  }

  "Insert one new activation" in {
    (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
    (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

    val result = activationRepository.insert(UUID.fromString("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"), "$s0$100801$Im7kWa5XcOMHIilt7VTonA==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7x5VdjMLFYE=")

    val testToken = TestValues.testUserTokenInsert

    val eitherToken = Await.result(result, Duration.Inf)

    val \/-(token) = eitherToken

    token.userId should be(testToken.userId)
    token.token should be(testToken.token)
  }

  "Delete one activation" in {
    (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

    val result = activationRepository.delete(UUID.fromString("f9aadc67-5e8b-48f3-b0a2-20a0d7d88477"))

    val testToken = TestValues.testUserToken.copy(createdAt = None)

    val eitherToken = Await.result(result, Duration.Inf)

    val \/-(token) = eitherToken

    token.userId should be(testToken.userId)
    token.token should be(testToken.token)
  }
}
