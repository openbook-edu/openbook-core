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
    val result = activationRepository.find(UUID.fromString("8b6dc674-d1ae-11e5-9080-08626681851d"))

    val testToken = TestValues.testUserToken

    val eitherToken = Await.result(result, Duration.Inf)

    val \/-(token) = eitherToken

    token.userId should be(testToken.userId)
    token.token should be(testToken.token)
    //    token.createdAt should be(testToken.createdAt)

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
    //    token.createdAt should be(testToken.createdAt)
  }

  "Insert one new activation" in {
    (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
    (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

    val result = activationRepository.insert(
      UUID.fromString("4d01347e-c592-4e5f-b09f-dd281b3d9b87"),
      "$s0$100801$Im7kWa5XcOMHIilt7A==$nO6OIL6lVz2OQ8vv5mNax1pgqSaaQlKG7xjMLFYE="
    )

    val testToken = TestValues.testUserTokenInsert

    val eitherToken = Await.result(result, Duration.Inf)

    val \/-(token) = eitherToken

    token.userId should be(testToken.userId)
    token.token should be(testToken.token)
  }

  "Delete one activation" in {
    (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

    val result = activationRepository.delete(UUID.fromString("7c62a526-d1b0-11e5-9080-08626681851d"))

    val testToken = TestValues.testUserTokenDelete.copy()

    val eitherToken = Await.result(result, Duration.Inf)

    val \/-(token) = eitherToken

    token.userId should be(testToken.userId)
    token.token should be(testToken.token)
  }
}
