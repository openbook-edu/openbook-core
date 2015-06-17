//import ca.shiftfocus.krispii.core.error.RepositoryError
//import ca.shiftfocus.krispii.core.models.document._
//import ca.shiftfocus.krispii.core.repositories.{ RevisionRepository, DocumentRepositoryPostgres }
//import com.github.mauricio.async.db.Connection
//import ws.kahn.ot.exceptions.IncompatibleDeltasException
//import ws.kahn.ot.{ Retain, InsertText, Delete, Delta }
//
//import scala.concurrent.{ Future, Await }
//import scala.concurrent.duration.Duration
//import scalaz.{ -\/, \/- }
//
//import org.scalatest._
//import Matchers._
//
//class DocumentRepositorySpec
//    extends TestEnvironment {
//  val revisionRepository = stub[RevisionRepository]
//  val documentRepository = new DocumentRepositoryPostgres(revisionRepository)
//
//  "DocumentRepository.find" should {
//    inSequence {
//      "find an individual document (the latest revision)" in {
//        val testDocument = TestValues.testDocumentC
//
//        val result = documentRepository.find(testDocument.id)
//        val eitherDocument = Await.result(result, Duration.Inf)
//        val \/-(document) = eitherDocument
//
//        document.id should be(testDocument.id)
//        document.version should be(testDocument.version)
//        document.ownerId should be(testDocument.ownerId)
//        document.title should be(testDocument.title)
//        document.delta should be(testDocument.delta)
//        document.createdAt.toString should be(testDocument.createdAt.toString)
//        document.updatedAt.toString should be(testDocument.updatedAt.toString)
//      }
//      "return RepositoryError.NoResults if Document doesn't exist (the latest revision)" in {
//        val testDocument = TestValues.testDocumentE
//
//        val result = documentRepository.find(testDocument.id)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("Could not find entity of type Document")))
//      }
//      "find an individual document (RepositoryError.NoResults)" in {
//        val testDocument = TestValues.testDocumentD
//
//        val revisionList = Vector(
//          TestValues.testPreviousRevisionD,
//          TestValues.testCurrentRevisionD
//        )
//
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument.copy(), *, testDocument.version, *) returns (Future.successful(\/-(revisionList)))
//
//        val result = documentRepository.find(testDocument.id, testDocument.version)
//        val eitherDocument = Await.result(result, Duration.Inf)
//        val \/-(document) = eitherDocument
//
//        document.id should be(testDocument.id)
//        document.version should be(testDocument.version)
//        document.ownerId should be(testDocument.ownerId)
//        document.title should be(testDocument.title)
//        document.delta should be(testDocument.delta)
//        document.createdAt.toString should be(testDocument.createdAt.toString)
//        document.updatedAt.toString should be(testDocument.updatedAt.toString)
//      }
//      "return RepositoryError.DatabaseError if deltas are incompatible (until revision)" in {
//        val testDocument = TestValues.testDocumentD
//
//        val revisionList = Vector(
//          TestValues.testPreviousRevisionD,
//          TestValues.testCurrentRevisionD.copy(
//            delta = Delta(IndexedSeq(
//              Delete(99),
//              InsertText("Hello"),
//              Retain(99)
//            ))
//          )
//        )
//
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument.copy(), *, testDocument.version, *) returns (Future.successful(\/-(revisionList)))
//
//        val result = documentRepository.find(testDocument.id, testDocument.version)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.DatabaseError("Incompatible deltas.", Some(IncompatibleDeltasException(revisionList(0).delta, revisionList(1).delta)))))
//      }
//    }
//  }
//
//  "DocumentRepository.insert" should {
//    inSequence {
//      "create new document" in {
//        val testDocument = TestValues.testDocumentE
//
//        val result = documentRepository.insert(testDocument)
//        val eitherDocument = Await.result(result, Duration.Inf)
//        val \/-(document) = eitherDocument
//
//        document.id should be(testDocument.id)
//        document.version should be(testDocument.version)
//        document.ownerId should be(testDocument.ownerId)
//        document.title should be(testDocument.title)
//        document.delta should be(testDocument.delta)
//      }
//      "return RepositoryError.PrimaryKeyConflict if already exists" in {
//        val testDocument = TestValues.testDocumentA
//
//        val result = documentRepository.insert(testDocument)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
//      }
//    }
//  }
//
//  "DocumentRepository.update" should {
//    inSequence {
//      "update a document" in {
//        val testDocument = TestValues.testDocumentB
//        val updatedDocument = testDocument.copy(
//          ownerId = TestValues.testUserG.id,
//          title = "new " + testDocument.title,
//          delta = Delta(IndexedSeq(
//            Delete(99),
//            InsertText("Bla bla"),
//            Retain(99)
//          ))
//        )
//
//        val result = documentRepository.update(updatedDocument)
//        val eitherDocument = Await.result(result, Duration.Inf)
//        val \/-(document) = eitherDocument
//
//        document.id should be(updatedDocument.id)
//        document.version should be(updatedDocument.version + 1)
//        document.ownerId should be(updatedDocument.ownerId)
//        document.title should be(updatedDocument.title)
//        document.delta should be(updatedDocument.delta)
//        document.createdAt.toString should be(updatedDocument.createdAt.toString)
//        document.updatedAt.toString should not be (updatedDocument.updatedAt.toString)
//      }
//      "return RepositoryError.NoResults if version is worng" in {
//        val testDocument = TestValues.testDocumentB
//        val updatedDocument = testDocument.copy(
//          version = 99L,
//          ownerId = TestValues.testUserG.id,
//          title = "new " + testDocument.title,
//          delta = Delta(IndexedSeq(
//            Delete(99),
//            InsertText("Bla bla"),
//            Retain(99)
//          ))
//        )
//
//        val result = documentRepository.update(updatedDocument)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("Could not find entity of type Document")))
//      }
//    }
//  }
//}
