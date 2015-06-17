//import ca.shiftfocus.krispii.core.error.RepositoryError
//import ca.shiftfocus.krispii.core.lib.ScalaCachePool
//import ca.shiftfocus.krispii.core.models.JournalEntry._
//import ca.shiftfocus.krispii.core.models._
//import ca.shiftfocus.krispii.core.repositories._
//import com.github.mauricio.async.db.Connection
//import java.util.UUID
//import org.joda.time.{ DateTimeZone, DateTime }
//import org.joda.time.format.DateTimeFormat
//import org.scalatest._
//import Matchers._
//
//import scala.collection.immutable.TreeMap
//import scala.concurrent.{ Future, Await }
//import scala.concurrent.duration.Duration
//import scalacache.ScalaCache
//import scalaz._
//
//class JournalRepositorySpec
//    extends TestEnvironment {
//  val userRepository = stub[UserRepository]
//  val projectRepository = stub[ProjectRepository]
//  val journalRepository = new JournalRepositoryPostgres(userRepository, projectRepository)
//
//  // TODO - internationalization of date time format
//  "JournalRepository.list" should {
//    inSequence {
//      "list Journal Entries by type" in {
//        val testUser = TestValues.testUserA
//        val testProject = TestValues.testProjectA
//        val journalEntryType = JournalEntryView.entryType
//
//        val testJournalEntryList = TreeMap[Int, JournalEntry](
//          0 -> TestValues.testJournalEntryA
//        )
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
//
//        val result = journalRepository.list(journalEntryType)
//        val eitherJournalEntryList = Await.result(result, Duration.Inf)
//        val \/-(journalEntries) = eitherJournalEntryList
//
//        journalEntries.size should be(testJournalEntryList.size)
//
//        testJournalEntryList.foreach {
//          case (key, journalEntry: JournalEntry) => {
//            journalEntries(key).id should be(journalEntry.id)
//            journalEntries(key).version should be(journalEntry.version)
//            journalEntries(key).userId should be(journalEntry.userId)
//            journalEntries(key).projectId should be(journalEntry.projectId)
//            journalEntries(key).entryType should be(journalEntry.entryType)
//            journalEntries(key).item should be(journalEntry.item)
//            journalEntries(key).message should be(journalEntry.message)
//            journalEntries(key).createdAt.toString should be(journalEntry.createdAt.toString)
//            journalEntries(key).updatedAt.toString should be(journalEntry.updatedAt.toString)
//          }
//        }
//      }
//      "list Journal Entries by user" in {
//        val testUser = TestValues.testUserA
//        val testProject = TestValues.testProjectA
//
//        val testJournalEntryList = TreeMap[Int, JournalEntry](
//          0 -> TestValues.testJournalEntryA,
//          1 -> TestValues.testJournalEntryB,
//          2 -> TestValues.testJournalEntryC,
//          3 -> TestValues.testJournalEntryD,
//          4 -> TestValues.testJournalEntryI
//        )
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
//
//        val result = journalRepository.list(testUser)
//        val eitherJournalEntryList = Await.result(result, Duration.Inf)
//        val \/-(journalEntries) = eitherJournalEntryList
//
//        journalEntries.size should be(testJournalEntryList.size)
//
//        testJournalEntryList.foreach {
//          case (key, journalEntry: JournalEntry) => {
//            journalEntries(key).id should be(journalEntry.id)
//            journalEntries(key).version should be(journalEntry.version)
//            journalEntries(key).userId should be(journalEntry.userId)
//            journalEntries(key).projectId should be(journalEntry.projectId)
//            journalEntries(key).entryType should be(journalEntry.entryType)
//            journalEntries(key).item should be(journalEntry.item)
//            journalEntries(key).message should be(journalEntry.message)
//            journalEntries(key).createdAt.toString should be(journalEntry.createdAt.toString)
//            journalEntries(key).updatedAt.toString should be(journalEntry.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if user ID doesn't exist" in {
//        val testUser = TestValues.testUserD
//
//        val result = journalRepository.list(testUser)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//      "list Journal Entries by start date" in {
//        val startDate = Option(TestValues.testJournalEntryC.createdAt)
//
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserA,
//          1 -> TestValues.testUserB
//        )
//        val testProjectList = TreeMap[Int, Project](
//          0 -> TestValues.testProjectA,
//          1 -> TestValues.testProjectB
//        )
//
//        val testJournalEntryList = TreeMap[Int, JournalEntry](
//          0 -> TestValues.testJournalEntryC,
//          1 -> TestValues.testJournalEntryD,
//          2 -> TestValues.testJournalEntryE,
//          3 -> TestValues.testJournalEntryF,
//          4 -> TestValues.testJournalEntryG,
//          5 -> TestValues.testJournalEntryH,
//          6 -> TestValues.testJournalEntryI
//        )
//
//        testUserList.foreach {
//          case (key, user: User) => {
//            (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (user.id, *, *) returns (Future.successful(\/-(user)))
//          }
//        }
//
//        testProjectList.foreach {
//          case (key, project: Project) => {
//            (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (project.id, *, *) returns (Future.successful(\/-(project)))
//          }
//        }
//
//        val result = journalRepository.list(startDate)
//        val eitherJournalEntryList = Await.result(result, Duration.Inf)
//        val \/-(journalEntries) = eitherJournalEntryList
//
//        journalEntries.size should be(testJournalEntryList.size)
//
//        testJournalEntryList.foreach {
//          case (key, journalEntry: JournalEntry) => {
//            journalEntries(key).id should be(journalEntry.id)
//            journalEntries(key).version should be(journalEntry.version)
//            journalEntries(key).userId should be(journalEntry.userId)
//            journalEntries(key).projectId should be(journalEntry.projectId)
//            journalEntries(key).entryType should be(journalEntry.entryType)
//            journalEntries(key).item should be(journalEntry.item)
//            journalEntries(key).message should be(journalEntry.message)
//            journalEntries(key).createdAt.toString should be(journalEntry.createdAt.toString)
//            journalEntries(key).updatedAt.toString should be(journalEntry.updatedAt.toString)
//          }
//        }
//      }
//      "list Journal Entries by end date" in {
//        val endDate = Option(TestValues.testJournalEntryE.createdAt)
//
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserA,
//          1 -> TestValues.testUserB
//        )
//        val testProjectList = TreeMap[Int, Project](
//          0 -> TestValues.testProjectA,
//          1 -> TestValues.testProjectB
//        )
//
//        val testJournalEntryList = TreeMap[Int, JournalEntry](
//          0 -> TestValues.testJournalEntryA,
//          1 -> TestValues.testJournalEntryB,
//          2 -> TestValues.testJournalEntryC,
//          3 -> TestValues.testJournalEntryD,
//          4 -> TestValues.testJournalEntryE
//        )
//
//        testUserList.foreach {
//          case (key, user: User) => {
//            (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (user.id, *, *) returns (Future.successful(\/-(user)))
//          }
//        }
//
//        testProjectList.foreach {
//          case (key, project: Project) => {
//            (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (project.id, *, *) returns (Future.successful(\/-(project)))
//          }
//        }
//
//        val result = journalRepository.list(endDate = endDate)
//        val eitherJournalEntryList = Await.result(result, Duration.Inf)
//        val \/-(journalEntries) = eitherJournalEntryList
//
//        journalEntries.size should be(testJournalEntryList.size)
//
//        testJournalEntryList.foreach {
//          case (key, journalEntry: JournalEntry) => {
//            journalEntries(key).id should be(journalEntry.id)
//            journalEntries(key).version should be(journalEntry.version)
//            journalEntries(key).userId should be(journalEntry.userId)
//            journalEntries(key).projectId should be(journalEntry.projectId)
//            journalEntries(key).entryType should be(journalEntry.entryType)
//            journalEntries(key).item should be(journalEntry.item)
//            journalEntries(key).message should be(journalEntry.message)
//            journalEntries(key).createdAt.toString should be(journalEntry.createdAt.toString)
//            journalEntries(key).updatedAt.toString should be(journalEntry.updatedAt.toString)
//          }
//        }
//      }
//      "list Journal Entries between start and end date" in {
//        val startDate = Option(TestValues.testJournalEntryD.createdAt)
//        val endDate = Option(TestValues.testJournalEntryH.createdAt)
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserA,
//          1 -> TestValues.testUserB
//        )
//        val testProjectList = TreeMap[Int, Project](
//          0 -> TestValues.testProjectA,
//          1 -> TestValues.testProjectB
//        )
//
//        val testJournalEntryList = TreeMap[Int, JournalEntry](
//          0 -> TestValues.testJournalEntryD,
//          1 -> TestValues.testJournalEntryE,
//          2 -> TestValues.testJournalEntryF,
//          3 -> TestValues.testJournalEntryG,
//          4 -> TestValues.testJournalEntryH
//        )
//
//        testUserList.foreach {
//          case (key, user: User) => {
//            (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (user.id, *, *) returns (Future.successful(\/-(user)))
//          }
//        }
//
//        testProjectList.foreach {
//          case (key, project: Project) => {
//            (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (project.id, *, *) returns (Future.successful(\/-(project)))
//          }
//        }
//
//        val result = journalRepository.list(startDate, endDate)
//        val eitherJournalEntryList = Await.result(result, Duration.Inf)
//        val \/-(journalEntries) = eitherJournalEntryList
//
//        journalEntries.size should be(testJournalEntryList.size)
//
//        testJournalEntryList.foreach {
//          case (key, journalEntry: JournalEntry) => {
//            journalEntries(key).id should be(journalEntry.id)
//            journalEntries(key).version should be(journalEntry.version)
//            journalEntries(key).userId should be(journalEntry.userId)
//            journalEntries(key).projectId should be(journalEntry.projectId)
//            journalEntries(key).entryType should be(journalEntry.entryType)
//            journalEntries(key).item should be(journalEntry.item)
//            journalEntries(key).message should be(journalEntry.message)
//            journalEntries(key).createdAt.toString should be(journalEntry.createdAt.toString)
//            journalEntries(key).updatedAt.toString should be(journalEntry.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if start and end dates are None" in {
//        val startDate = None
//        val endDate = None
//
//        val result = journalRepository.list(startDate, endDate)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//      "return empty Vector() if start date is greater than end date" in {
//        val startDate = Option(TestValues.testJournalEntryH.createdAt)
//        val endDate = Option(TestValues.testJournalEntryD.createdAt)
//
//        val result = journalRepository.list(startDate, endDate)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//    }
//  }
//
//  "JournalRepository.find" should {
//    inSequence {
//      "find JournalEntry by ID" in {
//        val testUser = TestValues.testUserA
//        val testProject = TestValues.testProjectA
//        val testJournalEntry = TestValues.testJournalEntryB
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
//
//        val result = journalRepository.find(testJournalEntry.id)
//        val eitherJournalEntry = Await.result(result, Duration.Inf)
//        val \/-(journalEntry) = eitherJournalEntry
//
//        journalEntry.id should be(testJournalEntry.id)
//        journalEntry.version should be(testJournalEntry.version)
//        journalEntry.userId should be(testJournalEntry.userId)
//        journalEntry.projectId should be(testJournalEntry.projectId)
//        journalEntry.entryType should be(testJournalEntry.entryType)
//        journalEntry.item should be(testJournalEntry.item)
//        journalEntry.message should be(testJournalEntry.message)
//        journalEntry.createdAt.toString should be(testJournalEntry.createdAt.toString)
//        journalEntry.updatedAt.toString should be(testJournalEntry.updatedAt.toString)
//      }
//      "return RepositoryError.NoResults if ID doesn't exist" in {
//        val journalEntryId = UUID.fromString("2f27a106-390f-4cbf-bcaf-5341ef987dd7")
//
//        val result = journalRepository.find(journalEntryId)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("Could not find entity of type Journal")))
//      }
//    }
//  }
//
//  "JournalRepository.insert" should {
//    inSequence {
//      "insert new Journal Entry" in {
//        val testUser = TestValues.testUserC
//        val testProject = TestValues.testProjectC
//        val testJournalEntry = TestValues.testJournalEntryJ
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
//
//        val result = journalRepository.insert(testJournalEntry)
//        val eitherJournalEntry = Await.result(result, Duration.Inf)
//        val \/-(journalEntry) = eitherJournalEntry
//
//        journalEntry.id should be(testJournalEntry.id)
//        journalEntry.version should be(1L)
//        journalEntry.userId should be(testJournalEntry.userId)
//        journalEntry.projectId should be(testJournalEntry.projectId)
//        journalEntry.entryType should be(testJournalEntry.entryType)
//        journalEntry.item should be(testJournalEntry.item)
//        journalEntry.message should be(testJournalEntry.message)
//      }
//      "reutrn RepositoryError.PrimaryKeyConflict if Journal Entry already exists" in {
//        val testUser = TestValues.testUserA
//        val testProject = TestValues.testProjectA
//        val testJournalEntry = TestValues.testJournalEntryA
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
//
//        val result = journalRepository.insert(testJournalEntry)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
//      }
//    }
//  }
//
//  "JournalRepository.delete" should {
//    inSequence {
//      "delete a Journal Entry" in {
//        val testUser = TestValues.testUserA
//        val testProject = TestValues.testProjectA
//        val testJournalEntry = TestValues.testJournalEntryB
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
//
//        val result = journalRepository.delete(testJournalEntry)
//        val eitherJournalEntry = Await.result(result, Duration.Inf)
//        val \/-(journalEntry) = eitherJournalEntry
//
//        journalEntry.id should be(testJournalEntry.id)
//        journalEntry.version should be(testJournalEntry.version)
//        journalEntry.userId should be(testJournalEntry.userId)
//        journalEntry.projectId should be(testJournalEntry.projectId)
//        journalEntry.entryType should be(testJournalEntry.entryType)
//        journalEntry.item should be(testJournalEntry.item)
//        journalEntry.message should be(testJournalEntry.message)
//        journalEntry.createdAt.toString should be(testJournalEntry.createdAt.toString)
//        journalEntry.updatedAt.toString should be(testJournalEntry.updatedAt.toString)
//      }
//      "return RepositoryError.NoResults if Journal Entry doesn't exist" in {
//        val unexistingEntry = TestValues.testJournalEntryJ
//
//        val result = journalRepository.delete(unexistingEntry)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("Could not find entity of type Journal")))
//      }
//      "delete all Journal Entries by type" in {
//        val testUserList = TreeMap[Int, User](
//          0 -> TestValues.testUserA,
//          1 -> TestValues.testUserB
//        )
//        val testProjectList = TreeMap[Int, Project](
//          0 -> TestValues.testProjectA,
//          1 -> TestValues.testProjectB
//        )
//
//        val testJournalEntryList = TreeMap[Int, JournalEntry](
//          0 -> TestValues.testJournalEntryH,
//          1 -> TestValues.testJournalEntryI
//        )
//
//        testUserList.foreach {
//          case (key, user: User) => {
//            (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (user.id, *, *) returns (Future.successful(\/-(user)))
//          }
//        }
//
//        testProjectList.foreach {
//          case (key, project: Project) => {
//            (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (project.id, *, *) returns (Future.successful(\/-(project)))
//          }
//        }
//
//        val result = journalRepository.delete(testJournalEntryList(0).entryType)
//        val eitherJournalEntryList = Await.result(result, Duration.Inf)
//        val \/-(journalEntries) = eitherJournalEntryList
//
//        journalEntries.size should be(testJournalEntryList.size)
//
//        testJournalEntryList.foreach {
//          case (key, journalEntry: JournalEntry) => {
//            journalEntries(key).id should be(journalEntry.id)
//            journalEntries(key).version should be(journalEntry.version)
//            journalEntries(key).userId should be(journalEntry.userId)
//            journalEntries(key).projectId should be(journalEntry.projectId)
//            journalEntries(key).entryType should be(journalEntry.entryType)
//            journalEntries(key).item should be(journalEntry.item)
//            journalEntries(key).message should be(journalEntry.message)
//            journalEntries(key).createdAt.toString should be(journalEntry.createdAt.toString)
//            journalEntries(key).updatedAt.toString should be(journalEntry.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if Journal Entry type doesn't exist" in {
//        val unexistingEntryType = "unexisting type"
//
//        val result = journalRepository.delete(unexistingEntryType)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//      "delete all Journal Entries that belong to the specific user" in {
//        val testUser = TestValues.testUserB
//        val testProject = TestValues.testProjectB
//
//        val testJournalEntryList = TreeMap[Int, JournalEntry](
//          0 -> TestValues.testJournalEntryE,
//          1 -> TestValues.testJournalEntryF,
//          2 -> TestValues.testJournalEntryG,
//          3 -> TestValues.testJournalEntryH
//        )
//
//        (userRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testUser.id, *, *) returns (Future.successful(\/-(testUser)))
//        (projectRepository.find(_: UUID)(_: Connection, _: ScalaCachePool)) when (testProject.id, *, *) returns (Future.successful(\/-(testProject)))
//
//        val result = journalRepository.delete(testUser)
//        val eitherJournalEntryList = Await.result(result, Duration.Inf)
//        val \/-(journalEntries) = eitherJournalEntryList
//
//        journalEntries.size should be(testJournalEntryList.size)
//
//        testJournalEntryList.foreach {
//          case (key, journalEntry: JournalEntry) => {
//            journalEntries(key).id should be(journalEntry.id)
//            journalEntries(key).version should be(journalEntry.version)
//            journalEntries(key).userId should be(journalEntry.userId)
//            journalEntries(key).projectId should be(journalEntry.projectId)
//            journalEntries(key).entryType should be(journalEntry.entryType)
//            journalEntries(key).item should be(journalEntry.item)
//            journalEntries(key).message should be(journalEntry.message)
//            journalEntries(key).createdAt.toString should be(journalEntry.createdAt.toString)
//            journalEntries(key).updatedAt.toString should be(journalEntry.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if User doesn't exist" in {
//        val unexistingUser = TestValues.testUserD
//
//        val result = journalRepository.delete(unexistingUser)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//    }
//  }
//}
