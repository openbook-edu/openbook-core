import ca.shiftfocus.krispii.core.error.{ RepositoryError, ServiceError }
import ca.shiftfocus.krispii.core.lib.ScalaCachePool
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection
import java.util.UUID

import org.scalatest._
import Matchers._

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz.{ -\/, \/- }

class TagServiceSpec
    extends TestEnvironment(writeToDb = false) {

  val db = stub[DB]
  val mockConnection = stub[Connection]
  val tagRepository = stub[TagRepository]
  val tagCategoryRepository = stub[TagCategoryRepository]

  val tagService = new TagServiceDefault(db, tagRepository, tagCategoryRepository) {
    override implicit def conn: Connection = mockConnection

    override def transactional[A](f: Connection => Future[A]): Future[A] = {
      f(mockConnection)
    }
  }

  "ProjectService.cloneTags" should {
    inSequence {
      "copy tags from one project to another" in {
        val toClone = TestValues.testProjectA
        val cloned = TestValues.testProjectC
        val tags = TreeMap[Int, ca.shiftfocus.krispii.core.models.Tag](
          0 -> TestValues.testTagA,
          1 -> TestValues.testTagB
        )

        (tagRepository.listByProjectId(_: UUID)(_: Connection)) when (toClone.id, *) returns (Future.successful(\/-(IndexedSeq[ca.shiftfocus.krispii.core.models.Tag](TestValues.testTagA, TestValues.testTagB))))
        (tagRepository.create(_: ca.shiftfocus.krispii.core.models.Tag)(_: Connection)) when (TestValues.testTagA, *) returns (Future.successful(\/-(TestValues.testTagA)))
        (tagRepository.create(_: ca.shiftfocus.krispii.core.models.Tag)(_: Connection)) when (TestValues.testTagB, *) returns (Future.successful(\/-(TestValues.testTagB)))

        val result = tagService.cloneTags(cloned.id, toClone.id)
        val \/-(clonedTags) = Await.result(result, Duration.Inf)

        tags.foreach {
          case (key, tag: ca.shiftfocus.krispii.core.models.Tag) => {
            clonedTags(key).lang should be(tag.lang)
            clonedTags(key).name should be(tag.name)
            clonedTags(key).category should be(tag.category)
          }
        }
      }
      "do nothing if there are no tags" in {
        val toClone = TestValues.testProjectB
        val cloned = TestValues.testProjectC

        (tagRepository.listByProjectId(_: UUID)(_: Connection)) when (toClone.id, *) returns (Future.successful(\/-(IndexedSeq[ca.shiftfocus.krispii.core.models.Tag]())))
        (tagRepository.create(_: ca.shiftfocus.krispii.core.models.Tag)(_: Connection)) when (TestValues.testTagB, *) returns (Future.successful(\/-(TestValues.testTagB)))

        val result = tagService.cloneTags(cloned.id, toClone.id)
        val \/-(clonedTags) = Await.result(result, Duration.Inf)

        clonedTags should be(Vector())
      }
    }
  }
}