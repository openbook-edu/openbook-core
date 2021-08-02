//import java.util.UUID
//
//import ca.shiftfocus.krispii.core.repositories._
//import org.joda.time.{ DateTime, DateTimeZone }
//import org.scalatest.Matchers._
//
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration
//import scalaz._
//
//class ConversationRepositorySpec
//    extends TestEnvironment {
//  val tagRepository = new TagRepositoryPostgres()
//  val userRepository = new UserRepositoryPostgres(tagRepository)
//  val roleRepository = new RoleRepositoryPostgres(userRepository)
//  val conversationRepository = new ConversationRepositoryPostgres(userRepository, roleRepository)
//
//  "ComponentRepository.list" should {
//    inSequence {
//      "list conversations after" in {
//        val time = System.currentTimeMillis
//        val result = conversationRepository.list(UUID.randomUUID(), new DateTime(time))
//        val eitherConversations = Await.result(result, Duration.Inf)
//        val \/-(conversations) = eitherConversations
//
//        conversations.foreach(conversation =>
//          println(Console.GREEN + conversation.id + " = " + conversation.title))
//
//        1 should be(1)
//      }
//    }
//  }
//}