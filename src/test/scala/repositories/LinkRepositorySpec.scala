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
///**
// * Created by vzaytseva on 23/02/16.
// */
//class LinkRepositorySpec extends TestEnvironment {
//  val linkRepository = new LinkRepositoryPostgres
//
//  "LinkRepository.create" should {
//    inSequence {
//      "Create a new registration link" in {
//        val testLink = TestValues.testLinkC
//        val result = linkRepository.create(testLink)
//        val eitherLink = Await.result(result, Duration.Inf)
//        val \/-(link) = eitherLink
//
//        link.link should be(testLink.link)
//        link.groupId should be(testLink.groupId)
//      }
//      "Return an unique key conflict if the link already exits" in {
//
//        val testLink = TestValues.testLinkA
//        val result = linkRepository.create(testLink)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
//      }
//    }
//  }
//
//  "LinkRepository.find" should {
//    inSequence {
//      "Return a valid link" in {
//        val testLink = TestValues.testLinkA
//        val result = linkRepository.find(testLink.link)
//        val eitherLink = Await.result(result, Duration.Inf)
//        val \/-(link) = eitherLink
//
//        link.link should be(testLink.link)
//        link.groupId should be(testLink.groupId)
//      }
//      "Return not found error if the link doest exists" in {
//        val testLink = TestValues.testLinkC
//        val result = linkRepository.find(testLink.link)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Link")))
//
//      }
//    }
//  }
//
//  "LinkRepository.findByCourse" should {
//    inSequence {
//      "Return a valid link" in {
//        val testLink = TestValues.testLinkA
//        val result = linkRepository.findByCourse(testLink.groupId)
//        val eitherLink = Await.result(result, Duration.Inf)
//        val \/-(link) = eitherLink
//
//        link.link should be(testLink.link)
//        link.groupId should be(testLink.groupId)
//      }
//      "Return not found error if the link doest exists" in {
//        val testLink = TestValues.testLinkC
//        val result = linkRepository.find(testLink.link)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Link")))
//
//      }
//    }
//  }
//
//  "LinkRepository.delete" should {
//    inSequence {
//      "delete a link from the database" in {
//        val testLink = TestValues.testLinkA
//        val result = linkRepository.delete(testLink)
//        val eitherLink = Await.result(result, Duration.Inf)
//        val \/-(link) = eitherLink
//
//        link.link should be(testLink.link)
//        link.groupId should be(testLink.groupId)
//      }
//      "return an error if the link doesnt exist" in {
//        val testLink = TestValues.testLinkC
//        val result = linkRepository.delete(testLink)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Link")))
//
//      }
//    }
//  }
//
//}
