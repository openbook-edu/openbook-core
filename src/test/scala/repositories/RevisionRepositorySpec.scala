import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.document.Revision
import ca.shiftfocus.krispii.core.repositories.RevisionRepositoryPostgres
import org.scalatest._
import Matchers._

import scala.collection.immutable.TreeMap
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz.{-\/, \/-}

class RevisionRepositorySpec
  extends TestEnvironment
{
  val revisionRepository = new RevisionRepositoryPostgres

  // TODO - test all testcases
  "RevisionRepository.list" should {
    inSequence {
      "list ALL revisions for a document" in {
        val testDocument = TestValues.testDocumentB

        val testRevisionList = TreeMap[Int, Revision](
          0 -> TestValues.testPreviousRevisionB,
          1 -> TestValues.testCurrentRevisionB
        )

        val result = revisionRepository.list(testDocument)
        val eitherRevisions = Await.result(result, Duration.Inf)
        val \/-(revisions) = eitherRevisions

        revisions.size should be(testRevisionList.size)

        testRevisionList.foreach {
          case (key, revision: Revision) => {
            revisions(key).documentId should be(revision.documentId)
            revisions(key).version should be(revision.version)
            revisions(key).authorId should be(revision.authorId)
            revisions(key).delta should be(revision.delta)
            revisions(key).createdAt.toString should be(revision.createdAt.toString)
          }
        }
      }
      "list revisions AFTER version (>) for a document" in {
        val testDocument = TestValues.testDocumentB

        val testRevisionList = TreeMap[Int, Revision](
          0 -> TestValues.testCurrentRevisionB
        )

        val result = revisionRepository.list(testDocument, afterVersion = testRevisionList(0).version - 1)
        val eitherRevisions = Await.result(result, Duration.Inf)
        val \/-(revisions) = eitherRevisions

        revisions.size should be(testRevisionList.size)

        testRevisionList.foreach {
          case (key, revision: Revision) => {
            revisions(key).documentId should be(revision.documentId)
            revisions(key).version should be(revision.version)
            revisions(key).authorId should be(revision.authorId)
            revisions(key).delta should be(revision.delta)
            revisions(key).createdAt.toString should be(revision.createdAt.toString)
          }
        }
      }
      "list revisions TO version (<=) for a document" in {
        val testDocument = TestValues.testDocumentB

        val testRevisionList = TreeMap[Int, Revision](
          0 -> TestValues.testPreviousRevisionB
        )

        val result = revisionRepository.list(testDocument, toVersion = testRevisionList(0).version)
        val eitherRevisions = Await.result(result, Duration.Inf)
        val \/-(revisions) = eitherRevisions

        revisions.size should be(testRevisionList.size)

        testRevisionList.foreach {
          case (key, revision: Revision) => {
            revisions(key).documentId should be(revision.documentId)
            revisions(key).version should be(revision.version)
            revisions(key).authorId should be(revision.authorId)
            revisions(key).delta should be(revision.delta)
            revisions(key).createdAt.toString should be(revision.createdAt.toString)
          }
        }
      }
      "list revisions BETWEEN (>= <=)versions for a document" in {
        val testDocument = TestValues.testDocumentB

        val testRevisionList = TreeMap[Int, Revision](
          0 -> TestValues.testPreviousRevisionB,
          1 -> TestValues.testCurrentRevisionB
        )

        val result = revisionRepository.list(testDocument, testRevisionList(0).version, testRevisionList(1).version)
        val eitherRevisions = Await.result(result, Duration.Inf)
        val \/-(revisions) = eitherRevisions

        revisions.size should be(testRevisionList.size)

        testRevisionList.foreach {
          case (key, revision: Revision) => {
            revisions(key).documentId should be(revision.documentId)
            revisions(key).version should be(revision.version)
            revisions(key).authorId should be(revision.authorId)
            revisions(key).delta should be(revision.delta)
            revisions(key).createdAt.toString should be(revision.createdAt.toString)
          }
        }
      }
    }
  }

  "RevisionRepository.find" should {
    inSequence {
      "find a single revision" in {
        val testDocument = TestValues.testDocumentC
        val testRevision = TestValues.testPreviousRevisionC

        val result = revisionRepository.find(testDocument, testRevision.version)
        val eitherRevision = Await.result(result, Duration.Inf)
        val \/-(revision) = eitherRevision

        revision.documentId should be(testRevision.documentId)
        revision.version should be(testRevision.version)
        revision.authorId should be(testRevision.authorId)
        revision.delta should be(testRevision.delta)
        revision.createdAt.toString should be(testRevision.createdAt.toString)
      }
    }
  }

  "RevisionRepository.insert" should {
    inSequence {
      "insert a new revision" in {
        val testRevision = TestValues.testUnexistingRevisionD

        val result = revisionRepository.insert(testRevision)
        val eitherRevision = Await.result(result, Duration.Inf)
        val \/-(revision) = eitherRevision

        revision.documentId should be(testRevision.documentId)
        revision.version should be(testRevision.version)
        revision.authorId should be(testRevision.authorId)
        revision.delta should be(testRevision.delta)
      }
      "return RepositoryError if document id and version already exist" in {
        val testRevision = TestValues.testCurrentRevisionA

        val result = revisionRepository.insert(testRevision)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }
}
