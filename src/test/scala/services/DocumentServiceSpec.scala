//import ca.shiftfocus.krispii.core.error.ServiceError
//import ca.shiftfocus.krispii.core.lib.ScalaCachePool
//import ca.shiftfocus.krispii.core.models.document.{ Document, Revision }
//import ca.shiftfocus.krispii.core.repositories._
//import ca.shiftfocus.krispii.core.services.DocumentServiceDefault
//import java.util.UUID
//import com.github.mauricio.async.db.Connection
//import org.joda.time.DateTime
//import org.scalatest._
//import Matchers._
//import ws.kahn.ot._
//import ws.kahn.ot.exceptions._
//import ca.shiftfocus.krispii.core.services.datasource.DB
//import scala.concurrent.{ Await, Future }
//import scala.concurrent.duration.Duration
//import scalaz._
//
//class DocumentServiceSpec
//    extends TestEnvironment(writeToDb = false) {
//
//  val db = stub[DB]
//  val mockConnection = stub[Connection]
//  val userRepository = stub[UserRepository]
//  val documentRepository = stub[DocumentRepository]
//  val revisionRepository = stub[RevisionRepository]
//
//  val documentService = new DocumentServiceDefault(db, cache, userRepository, documentRepository, revisionRepository) {
//    override implicit def conn: Connection = mockConnection
//
//    override def transactional[A](f: Connection => Future[A]): Future[A] = {
//      f(mockConnection)
//    }
//  }
//
//  // TODO - add testcases with predicate
//  "DocumentService.update" should {
//    inSequence {
//      "return ServiceError.OfflineLockFail if versions don't match" in {
//        val testDocument = TestValues.testDocumentA
//        val testUser = TestValues.testUserC
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)) when (testDocument.id, *, *) returns (Future.successful(\/-(testDocument)))
//
//        val result = documentService.update(testDocument.id, testDocument.version + 1, testUser, IndexedSeq(testUser), testDocument.title)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
//      }
//    }
//  }
//
//  "DocumentService.push" should {
//    inSequence {
//      "push a new revision if there are no recent revisions" in {
//        val expectedDelta = Delta(IndexedSeq(InsertText("Hello Sam")))
//
//        val pushedDelta = Delta(IndexedSeq(
//          InsertText("Hello Sam")
//        ))
//
//        // If Revisions are not found then these too are equal
//        val transformedDelta = pushedDelta
//
//        val testDocument = TestValues.testDocumentA.copy(delta = Delta(IndexedSeq.empty[Operation]))
//        val updatedDocument = testDocument.copy(delta = expectedDelta)
//        val testAuthor = TestValues.testUserC
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq.empty[Revision])
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 1), *, *) returns (Future.successful(\/-(IndexedSeq.empty[Revision])))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 1), testAuthor, pushedDelta)
//        val \/-(pushResult) = Await.result(result, Duration.Inf)
//
//        pushResult should be(testPushResult)
//      }
//      "push a new revision if recent revision is only one" in {
//        val latestDelta = Delta(IndexedSeq(InsertText("Hello Sam")))
//        val expectedDelta = Delta(IndexedSeq(InsertText("Hello dear Sam")))
//        val recentDelta = Delta(IndexedSeq(
//          Retain(5),
//          InsertText(" Sam")
//        ))
//        val pushedDelta = Delta(IndexedSeq(
//          Retain(5),
//          InsertText(" dear")
//        ))
//        val transformedDelta = Delta(IndexedSeq(
//          Retain(5),
//          InsertText(" dear"),
//          Retain(4)
//        ))
//
//        val testDocument = TestValues.testDocumentA.copy(delta = latestDelta)
//        val updatedDocument = testDocument.copy(delta = expectedDelta)
//        val testAuthor = TestValues.testUserC
//
//        val recentRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          delta = recentDelta,
//          createdAt = new DateTime
//        )
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq(recentRevision))
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 1), *, *) returns (Future.successful(\/-(IndexedSeq(recentRevision))))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 1), testAuthor, pushedDelta)
//        val \/-(pushResult) = Await.result(result, Duration.Inf)
//
//        pushResult should be(testPushResult)
//      }
//      "push a new revision with BooleanAttribute" in {
//        val latestDelta = Delta(IndexedSeq(InsertText("Hello Sam")))
//        val expectedDelta = Delta(IndexedSeq(
//          InsertText("Hello"),
//          InsertText(" dear", Some(Map("bold" -> BooleanAttribute(true)))),
//          InsertText(" Sam", None)
//        ))
//
//        val recentDelta = Delta(IndexedSeq(
//          Retain(5),
//          InsertText(" Sam")
//        ))
//
//        val pushedDelta = Delta(IndexedSeq(
//          Retain(5),
//          InsertText(" dear", Some(Map("bold" -> BooleanAttribute(true))))
//        ))
//
//        val transformedDelta = Delta(IndexedSeq(
//          Retain(5),
//          InsertText(" dear", Some(Map("bold" -> BooleanAttribute(true)))),
//          Retain(4)
//        ))
//
//        val testDocument = TestValues.testDocumentA.copy(delta = latestDelta)
//        val updatedDocument = testDocument.copy(delta = expectedDelta)
//        val testAuthor = TestValues.testUserC
//
//        val recentRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          delta = recentDelta,
//          createdAt = new DateTime
//        )
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq(recentRevision))
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 1), *, *) returns (Future.successful(\/-(IndexedSeq(recentRevision))))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 1), testAuthor, pushedDelta)
//        val \/-(pushResult) = Await.result(result, Duration.Inf)
//
//        pushResult should be(testPushResult)
//      }
//      "push a new revision if there are more then one recent revisions (Retain, InsertText, InserteCode(0), Delete are covered)" in {
//        val latestDelta = Delta(IndexedSeq(InsertText("Hello Mr. Sam")))
//        val expectedDelta = Delta(IndexedSeq(InsertText("Dear,"), InsertCode(0), InsertText("Lorem Ipsum Mr. Sam")))
//
//        val testDocument = TestValues.testDocumentA.copy(delta = latestDelta)
//        val updatedDocument = testDocument.copy(delta = expectedDelta)
//        val testAuthor = TestValues.testUserC
//
//        val recentRevision1 = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          createdAt = new DateTime,
//          delta = Delta(IndexedSeq(
//            Retain(5),
//            InsertText(" Mr.")
//          ))
//        )
//
//        val recentRevision2 = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          createdAt = new DateTime,
//          delta = Delta(IndexedSeq(
//            Retain(9),
//            InsertText(" Sam")
//          ))
//        )
//
//        val pushedDelta = Delta(IndexedSeq(
//          Delete(5),
//          InsertText("Dear,"),
//          InsertCode(0),
//          InsertText("Lorem Ipsum")
//        ))
//
//        val transformedDelta = Delta(IndexedSeq(
//          Delete(5),
//          InsertText("Dear,"),
//          InsertCode(0),
//          InsertText("Lorem Ipsum"),
//          Retain(8)
//        ))
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq(recentRevision1, recentRevision2))
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 2), *, *) returns (Future.successful(\/-(IndexedSeq(recentRevision1, recentRevision2))))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 2), testAuthor, pushedDelta)
//        val \/-(pushResult) = Await.result(result, Duration.Inf)
//
//        pushResult should be(testPushResult)
//      }
//      "throw ws.kahn.ot.exceptions.IncompatibleDeltasException if recent Revision has wrong Delta" in {
//        val latestDelta = Delta(IndexedSeq(InsertText("Hello Mr. Sam")))
//        val expectedDelta = Delta(IndexedSeq(InsertText("Dear Mr. Sam")))
//
//        val testDocument = TestValues.testDocumentA.copy(delta = latestDelta)
//        val updatedDocument = testDocument.copy(delta = expectedDelta)
//        val testAuthor = TestValues.testUserC
//
//        val recentRevision1 = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          createdAt = new DateTime,
//          delta = Delta(IndexedSeq(
//            Retain(5),
//            InsertText(" Mr.")
//          ))
//        )
//
//        val recentRevision2 = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          createdAt = new DateTime,
//          delta = Delta(IndexedSeq(
//            Retain(8), // Changed from Retain(9) to throw an Exception
//            InsertText(" Sam")
//          ))
//        )
//
//        val pushedDelta = Delta(IndexedSeq(
//          Delete(5),
//          InsertText("Dear")
//        ))
//
//        val transformedDelta = Delta(IndexedSeq(
//          Delete(5),
//          InsertText("Dear"),
//          Retain(8)
//        ))
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq(recentRevision1, recentRevision2))
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 2), *, *) returns (Future.successful(\/-(IndexedSeq(recentRevision1, recentRevision2))))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 2), testAuthor, pushedDelta)
//        an[IncompatibleDeltasException] should be thrownBy (Await.result(result, Duration.Inf))
//      }
//      "throw java.lang.IndexOutOfBoundsException if an Operation is missing in the pushed Delta" in {
//        val latestDelta = Delta(IndexedSeq(InsertText("Hello Mr. Sam")))
//        val expectedDelta = Delta(IndexedSeq(InsertText("Dearo Mr. Sam")))
//
//        val testDocument = TestValues.testDocumentA.copy(delta = latestDelta)
//        val updatedDocument = testDocument.copy(delta = expectedDelta)
//        val testAuthor = TestValues.testUserC
//
//        val recentRevision1 = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          createdAt = new DateTime,
//          delta = Delta(IndexedSeq(
//            Retain(5),
//            InsertText(" Mr.")
//          ))
//        )
//
//        val recentRevision2 = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          createdAt = new DateTime,
//          delta = Delta(IndexedSeq(
//            Retain(9),
//            InsertText(" Sam")
//          ))
//        )
//
//        val pushedDelta = Delta(IndexedSeq(
//          Delete(4),
//          InsertText("Dear")
//        //          Retain(1) // Commented to throw an Exception
//        ))
//
//        val transformedDelta = Delta(IndexedSeq(
//          Delete(4),
//          InsertText("Dear"),
//          Retain(9)
//        ))
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq(recentRevision1, recentRevision2))
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 2), *, *) returns (Future.successful(\/-(IndexedSeq(recentRevision1, recentRevision2))))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 2), testAuthor, pushedDelta)
//        an[java.lang.IndexOutOfBoundsException] should be thrownBy (Await.result(result, Duration.Inf))
//      }
//      "throw ws.kahn.ot.exceptions.IncompatibleDeltasException if recent Revisions exist but they haven't been found" in {
//        val latestDelta = Delta(IndexedSeq(InsertText("Hello Mr. Sam")))
//        val expectedDelta = Delta(IndexedSeq(InsertText("Dearo Mr. Sam")))
//
//        val testDocument = TestValues.testDocumentA.copy(delta = latestDelta)
//        val updatedDocument = testDocument.copy(delta = expectedDelta)
//        val testAuthor = TestValues.testUserC
//
//        val pushedDelta = Delta(IndexedSeq(
//          Delete(4),
//          InsertText("Dear"),
//          Retain(1)
//        ))
//
//        // If Revisions are not found then these too are equal
//        val transformedDelta = pushedDelta
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq())
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 2), *, *) returns (Future.successful(\/-(IndexedSeq())))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 2), testAuthor, pushedDelta)
//        an[IncompatibleDeltasException] should be thrownBy (Await.result(result, Duration.Inf))
//      }
//      "return ServiceError.BadInput if PushedDelta is empty and Document.Delta is empty too" in {
//        val expectedDelta = Delta(IndexedSeq(InsertText("Hello Sam")))
//
//        val pushedDelta = Delta(IndexedSeq())
//
//        // If Revisions are not found then these too are equal
//        val transformedDelta = pushedDelta
//
//        val testDocument = TestValues.testDocumentA
//        val updatedDocument = testDocument
//        val testAuthor = TestValues.testUserC
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq.empty[Revision])
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 1), *, *) returns (Future.successful(\/-(IndexedSeq.empty[Revision])))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 1), testAuthor, pushedDelta)
//        Await.result(result, Duration.Inf) shouldBe a[-\/[ServiceError.BadInput]]
//      }
//      "return ServiceError.BadInput if PushedDelta is empty, but Document.Delta is not" in {
//        val latestDelta = Delta(IndexedSeq(InsertText("Hello Sam")))
//        val expectedDelta = Delta(IndexedSeq(InsertText("Hello Sam")))
//        val recentDelta = Delta(IndexedSeq(
//          Retain(5),
//          InsertText(" Sam")
//        ))
//        val pushedDelta = Delta(IndexedSeq())
//
//        val transformedDelta = Delta(IndexedSeq(
//          Retain(9)
//        ))
//
//        val testDocument = TestValues.testDocumentA.copy(delta = latestDelta)
//        val updatedDocument = testDocument.copy(delta = expectedDelta)
//        val testAuthor = TestValues.testUserC
//
//        val recentRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          delta = recentDelta,
//          createdAt = new DateTime
//        )
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq(recentRevision))
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 1), *, *) returns (Future.successful(\/-(IndexedSeq(recentRevision))))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 1), testAuthor, pushedDelta)
//        Await.result(result, Duration.Inf) shouldBe a[-\/[ServiceError.BadInput]]
//      }
//      "return ServiceError.BadInput if Document.Delta contains not only inserts" in {
//        val latestDelta = Delta(IndexedSeq(
//          InsertText("Hello"),
//          Retain(1),
//          InsertText(" Sam")
//        ))
//
//        val expectedDelta = Delta(IndexedSeq(InsertText("Hello dear Sam")))
//
//        val recentDelta = Delta(IndexedSeq(
//          Retain(6),
//          InsertText(" Sam")
//        ))
//
//        val pushedDelta = Delta(IndexedSeq(
//          Retain(6),
//          InsertText(" dear")
//        ))
//
//        val transformedDelta = Delta(IndexedSeq(
//          Retain(6),
//          InsertText(" dear"),
//          Retain(4)
//        ))
//
//        val testDocument = TestValues.testDocumentA.copy(delta = latestDelta)
//        val updatedDocument = testDocument.copy(delta = expectedDelta)
//        val testAuthor = TestValues.testUserC
//
//        val recentRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version,
//          authorId = testAuthor.id,
//          delta = recentDelta,
//          createdAt = new DateTime
//        )
//
//        val pushedRevision = Revision(
//          documentId = testDocument.id,
//          version = testDocument.version + 1,
//          authorId = testAuthor.id,
//          delta = transformedDelta,
//          createdAt = new DateTime
//        )
//
//        val testPushResult = documentService.PushResult(updatedDocument, pushedRevision, IndexedSeq(recentRevision))
//
//        (documentRepository.find(_: UUID, _: Long)(_: Connection)).when(testDocument.id, 0L, *).returns(Future.successful(\/-(testDocument)))
//        (revisionRepository.list(_: Document, _: Long, _: Long)(_: Connection)) when (testDocument, (testDocument.version - 1), *, *) returns (Future.successful(\/-(IndexedSeq(recentRevision))))
//        (documentRepository.update(_: Document)(_: Connection)) when (updatedDocument, *) returns (Future.successful(\/-(updatedDocument)))
//        (revisionRepository.insert(_: Revision)(_: Connection)) when (pushedRevision, *) returns (Future.successful(\/-(pushedRevision)))
//
//        val result = documentService.push(testDocument.id, (testDocument.version - 1), testAuthor, pushedDelta)
//        Await.result(result, Duration.Inf) shouldBe a[-\/[ServiceError.BadInput]]
//      }
//    }
//  }
//}
