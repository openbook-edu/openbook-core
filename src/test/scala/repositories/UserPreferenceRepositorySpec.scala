//import ca.shiftfocus.krispii.core.error.RepositoryError
//import ca.shiftfocus.krispii.core.models.UserPreference
//import ca.shiftfocus.krispii.core.repositories.UserPreferenceRepositoryPostgres
//
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration
//import scalaz.{ -\/, \/- }
//import org.scalatest.Matchers._
//import org.scalatest._
//
//import scala.collection.immutable.TreeMap
//
//class UserPreferenceRepositorySpec extends TestEnvironment {
//  val userPrefRepository = new UserPreferenceRepositoryPostgres
//
//  "UserPreferenceRepository.list" should {
//    inSequence {
//      "list all preferences for a user" in {
//        val testUser = TestValues.testUserA
//        val userPrefList = TreeMap[Int, UserPreference](
//          0 -> TestValues.testUserPrefA
//        )
//
//        val result = userPrefRepository.list(testUser.id)
//        val eitherUserPrefs = Await.result(result, Duration.Inf)
//        val \/-(userPrefs) = eitherUserPrefs
//
//        userPrefs.size should be(userPrefList.size)
//
//        userPrefList.foreach {
//          case (key, userPref: UserPreference) => {
//            userPrefList(key).userId should be(userPref.userId)
//            userPrefList(key).prefName should be(userPref.prefName)
//            userPrefList(key).state should be(userPref.state)
//          }
//        }
//      }
//    }
//  }
//
//  "UserPreferenceRepository.set (create)" should {
//    inSequence {
//      "create new preference for the user" in {
//        val testUserPref = TestValues.testUserPrefB
//
//        val result = userPrefRepository.set(testUserPref)
//        val eitherUserPref = Await.result(result, Duration.Inf)
//        val \/-(userPref) = eitherUserPref
//
//        userPref.userId should be(testUserPref.userId)
//        userPref.prefName should be(testUserPref.prefName)
//        userPref.state should be(testUserPref.state)
//      }
//      "return RepositoryError.NoResults when create pref with unexisting name" in {
//        val testUserPref = TestValues.testUserPrefB.copy(
//          prefName = "unexisting_pref"
//        )
//
//        val result = userPrefRepository.set(testUserPref)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type UserPreference")))
//      }
//      "return RepositoryError.NoResults when create pref with not allowed value" in {
//        val testUserPref = TestValues.testUserPrefB.copy(
//          state = "unexisting_state"
//        )
//
//        val result = userPrefRepository.set(testUserPref)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type UserPreference")))
//      }
//    }
//  }
//
//  "UserPreferenceRepository.set (update)" should {
//    inSequence {
//      "update preference for the user" in {
//        val testUserPref = TestValues.testUserPrefA
//        val updatedUserPref = testUserPref.copy(
//          state = "fr"
//        )
//
//        val result = userPrefRepository.set(updatedUserPref)
//        val eitherUserPref = Await.result(result, Duration.Inf)
//        val \/-(userPref) = eitherUserPref
//
//        userPref.userId should be(updatedUserPref.userId)
//        userPref.prefName should be(updatedUserPref.prefName)
//        userPref.state should be(updatedUserPref.state)
//      }
//      "return RepositoryError.NoResults when update pref with unexisting name" in {
//        val testUserPref = TestValues.testUserPrefA
//        val updatedUserPref = testUserPref.copy(
//          prefName = "unexisting_pref"
//        )
//
//        val result = userPrefRepository.set(updatedUserPref)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type UserPreference")))
//      }
//      "return RepositoryError.NoResults when update pref with not allowed value" in {
//        val testUserPref = TestValues.testUserPrefA
//        val updatedUserPref = testUserPref.copy(
//          state = "unexisting_state"
//        )
//
//        val result = userPrefRepository.set(updatedUserPref)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type UserPreference")))
//      }
//    }
//  }
//}
