//import ca.shiftfocus.krispii.core.error.RepositoryError
//import ca.shiftfocus.krispii.core.models._
//import ca.shiftfocus.krispii.core.models.document._
//import ca.shiftfocus.krispii.core.repositories._
//import java.util.UUID
//import com.github.mauricio.async.db.Connection
//import org.scalatest._
//import Matchers._
//
//import scala.collection.immutable.TreeMap
//import scala.concurrent.duration.Duration
//import scala.concurrent.{ Await, Future }
//import scalaz.{ -\/, \/- }
//
//class ProjectScratchpadRepositorySpec extends TestEnvironment {
//  val documentRepository = stub[DocumentRepository]
//  val projectScratchpadRepository = new ProjectScratchpadRepositoryPostgres(documentRepository)
//
//  "ProjectScratchpadRepository.list" should {
//    inSequence {
//      "list all the notes from the user" in {
//        val testUser = TestValues.testUserA
//
//        val testProjectScratchpadList = TreeMap[Int, ProjectScratchpad](
//          0 -> TestValues.testProjectScratchpadA,
//          1 -> TestValues.testProjectScratchpadB
//        )
//
//        val testDocumentList = TreeMap[Int, Document](
//          0 -> TestValues.testDocumentJ,
//          1 -> TestValues.testDocumentF
//        )
//
//        testDocumentList.foreach {
//          case (key, document: Document) => {
//            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
//          }
//        }
//
//        val result = projectScratchpadRepository.list(testUser)
//        val eitherProjectScratchpads = Await.result(result, Duration.Inf)
//        val \/-(projectScratchpads) = eitherProjectScratchpads
//
//        projectScratchpads.size should be(testProjectScratchpadList.size)
//        testProjectScratchpadList.foreach {
//          case (key, projectScratchpad: ProjectScratchpad) => {
//            projectScratchpads(key).userId should be(projectScratchpad.userId)
//            projectScratchpads(key).projectId should be(projectScratchpad.projectId)
//            projectScratchpads(key).version should be(projectScratchpad.version)
//            projectScratchpads(key).documentId should be(projectScratchpad.documentId)
//            projectScratchpads(key).createdAt.toString should be(projectScratchpad.createdAt.toString)
//            projectScratchpads(key).updatedAt.toString should be(projectScratchpad.updatedAt.toString)
//          }
//        }
//      }
//      "return empty Vector() if User doesn't exist (for each project)" in {
//        val testUser = TestValues.testUserD
//        val testProject = TestValues.testProjectA
//
//        val testProjectScratchpadList = TreeMap[Int, ProjectScratchpad](
//          0 -> TestValues.testProjectScratchpadA,
//          1 -> TestValues.testProjectScratchpadB
//        )
//
//        val testDocumentList = TreeMap[Int, Document](
//          0 -> TestValues.testDocumentK,
//          1 -> TestValues.testDocumentM
//        )
//
//        testDocumentList.foreach {
//          case (key, document: Document) => {
//            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
//          }
//        }
//
//        val result = projectScratchpadRepository.list(testUser)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//    }
//  }
//  "ProjectScratchpadRepository.find" should {
//    inSequence {
//      "return a document for a user and a project" in {
//        val testUser = TestValues.testUserA
//        val testProject = TestValues.testProjectA
//
//        val testProjectScratchpad = TestValues.testProjectScratchpadA
//
//        val testDocumentList = TreeMap[Int, Document](
//          0 -> TestValues.testDocumentJ,
//          1 -> TestValues.testDocumentF
//        )
//
//        testDocumentList.foreach {
//          case (key, document: Document) => {
//            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
//          }
//        }
//
//        val result = projectScratchpadRepository.find(testUser, testProject)
//        val eitherProjectScratchpads = Await.result(result, Duration.Inf)
//        val \/-(projectScratchpads) = eitherProjectScratchpads
//
//        projectScratchpads.userId should be(testProjectScratchpad.userId)
//      }
//
//      "return no results if it doesn't exist" in {
//        val testUser = TestValues.testUserA
//        val testProject = TestValues.testProjectC
//        val result = projectScratchpadRepository.find(testUser, testProject)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type ProjectScratchpad")))
//      }
//
//      "return an error if user doesn't exist" in {
//        val testUser = TestValues.testUserD
//        val testProject = TestValues.testProjectC
//        val result = projectScratchpadRepository.find(testUser, testProject)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type ProjectScratchpad")))
//      }
//    }
//  }
//  "ProjectScratchpadRepository.insert" should {
//    inSequence {
//      "insert a new ProjectScratchpad" in {
//        val testProjectScratchpad = TestValues.testProjectScratchpadX
//        val testDocument = TestValues.testDocumentJ
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))
//
//        val result = projectScratchpadRepository.insert(testProjectScratchpad)
//        val eitherProjectScratchpad = Await.result(result, Duration.Inf)
//        val \/-(projectScratchpad) = eitherProjectScratchpad
//
//        projectScratchpad.userId should be(testProjectScratchpad.userId)
//        projectScratchpad.projectId should be(testProjectScratchpad.projectId)
//        projectScratchpad.version should be(testProjectScratchpad.version)
//        projectScratchpad.documentId should be(testProjectScratchpad.documentId)
//        projectScratchpad.createdAt.toString should be(testProjectScratchpad.createdAt.toString)
//        projectScratchpad.updatedAt.toString should be(testProjectScratchpad.updatedAt.toString)
//      }
//      "return PrimaryKeyConflict if Project Scratchpad already exists" in {
//        val testProjectScratchpad = TestValues.testProjectScratchpadA
//        val testDocument = TestValues.testDocumentJ
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))
//
//        val result = projectScratchpadRepository.insert(testProjectScratchpad)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
//      }
//    }
//  }
//
//  "ProjectScratchpadRepository.delete" should {
//    inSequence {
//      "deletes a project scratchpad" in {
//        val testProjectScratchpad = TestValues.testProjectScratchpadA
//        val testDocument = TestValues.testDocumentJ
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))
//
//        val result = projectScratchpadRepository.delete(testProjectScratchpad)
//        val eitherProjectScratchpad = Await.result(result, Duration.Inf)
//        val \/-(projectScratchpad) = eitherProjectScratchpad
//
//        projectScratchpad.userId should be(testProjectScratchpad.userId)
//        projectScratchpad.projectId should be(testProjectScratchpad.projectId)
//        projectScratchpad.version should be(testProjectScratchpad.version)
//        projectScratchpad.documentId should be(testProjectScratchpad.documentId)
//        projectScratchpad.createdAt.toString should be(testProjectScratchpad.createdAt.toString)
//        projectScratchpad.updatedAt.toString should be(testProjectScratchpad.updatedAt.toString)
//      }
//      "rerurn RepositoryError.NoResults if project scratchpad doesn't exist" in {
//        val testProjectScratchpad = TestValues.testProjectScratchpadX
//        val testDocument = TestValues.testDocumentJ
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))
//
//        val result = projectScratchpadRepository.delete(testProjectScratchpad)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type ProjectScratchpad")))
//      }
//
//      "rerurn empty Vector() if project doesn't exist" in {
//        val testProject = TestValues.testProjectD
//
//        val testProjectScratchpadList = TreeMap[Int, ProjectScratchpad](
//          0 -> TestValues.testProjectScratchpad,
//          1 -> TestValues.testProjectScratchpadD
//        )
//
//        val testDocumentList = TreeMap[Int, Document](
//          0 -> TestValues.testDocumentN,
//          1 -> TestValues.testDocumentO
//        )
//
//        testDocumentList.foreach {
//          case (key, document: Document) => {
//            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (document.id, *, *) returns (Future.successful(\/-(document)))
//          }
//        }
//
//        val result = projectScratchpadRepository.delete(testProject)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//    }
//  }
//}
