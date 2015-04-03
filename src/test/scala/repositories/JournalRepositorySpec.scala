import ca.shiftfocus.krispii.core.models.JournalEntry._
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories._
import com.github.mauricio.async.db.Connection
import ca.shiftfocus.uuid.UUID
import org.scalatest._
import Matchers._

import scala.collection.immutable.TreeMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scalaz._


class JournalRepositorySpec
 extends TestEnvironment {

  val userRepository    = stub[UserRepository]
  val projectRepository = stub[ProjectRepository]
  val journalRepository = new JournalRepositoryPostgres(userRepository, projectRepository)

  "JournalRepository.list" should {
    inSequence {
      "list Journal Entries by type" in {
        val testJournalEntryList = TreeMap[Int, JournalEntry](
          0 -> TestValues.testJournalEntryB
        )

        (userRepository.find(_: UUID)(_: Connection)) when(TestValues.testUserA.id, *) returns(Future.successful(\/-(TestValues.testUserA)))
        (projectRepository.find(_: UUID)(_: Connection)) when(TestValues.testProjectA.id, *) returns(Future.successful(\/-(TestValues.testProjectA)))

        val result = journalRepository.list(JournalEntryView.entryType)
        val eitherJournalEntryList = Await.result(result, Duration.Inf)
        val \/-(journalEntries) = eitherJournalEntryList
console_log(journalEntries)
        journalEntries.size should be(testJournalEntryList.size)
      }
    }
  }
}
