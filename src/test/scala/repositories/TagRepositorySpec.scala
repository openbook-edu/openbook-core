import java.util.UUID

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models._
import ca.shiftfocus.krispii.core.repositories.{ RoleRepositoryPostgres, TagRepositoryPostgres, UserRepositoryPostgres }
import org.scalatest.Matchers._
import org.scalatest._

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz.{ \/, \/-, -\/, EitherT }

class TagRepositorySpec
    extends TestEnvironment {
  val tagRepository = new TagRepositoryPostgres
  "TagRepository.create" should {
    inSequence {
      "create a new tag" in {
        val testTag = TestValues.testTagX
        val result = tagRepository.create(testTag)
        val eitherTag = Await.result(result, Duration.Inf)
        val \/-(tag) = eitherTag

        tag.id should be(testTag.id)
        tag.name should be(testTag.name)
      }
      "return unique key conflict if tag already exists" in {
        val result = tagRepository.create(TestValues.testTagA)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }

  "TagRepository.delete" should {
    inSequence {
      "delete the tag if it doesn't have references in other tables" in {
        val testTag = TestValues.testTagD

        val result = tagRepository.delete(testTag.id)
        Await.result(result, Duration.Inf) should be(\/-(testTag))
      }

      "return an error if a tag is still attached to a project" in {
        //tag_id,project_tags_tag_id_fkey
        val result = tagRepository.delete(TestValues.testTagA.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("tag_id", "project_tags_tag_id_fkey")))
      }

      "return not found if deleting unexisting tag" in {
        val result = tagRepository.delete(TestValues.testTagX.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Tag")))
      }
    }
  }

  "TagRepository.untag" should {
    inSequence {
      "remove a tag from a project" in {

        val result = tagRepository.untag(TestValues.testProjectA.id, TestValues.testTagA.id)
        Await.result(result, Duration.Inf) should be(\/-(()))
      }
      "return RepositoryError.NoResults if something doesn't exist" in {
        val testProject = TestValues.testProjectH
        val testTag = TestValues.testTagX

        val result = tagRepository.untag(testProject.id, testTag.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("Could not remove the tag")))
      }
    }
  }

  "TagRepository.trigramSearch" should {
    inSequence {
      "find the tags given the keyword" in {

        val result = tagRepository.trigramSearch("sed")
        val eitherTags = Await.result(result, Duration.Inf)
        val \/-(tags) = eitherTags

        tags.size should be(2)

      }
      "return no results if the keyword doesn't match anything" in {
        val result = tagRepository.trigramSearch("coco")
        val eitherTags = Await.result(result, Duration.Inf)
        val \/-(tags) = eitherTags

        tags.size should be(0)
      }
    }
  }

  "TagRepository.listByProject" should {
    inSequence {
      "list tags for project" in {
        val tagsList = TreeMap[Int, ca.shiftfocus.krispii.core.models.Tag](
          0 -> TestValues.testTagA,
          1 -> TestValues.testTagB
        )

        val result = tagRepository.listByProjectId(TestValues.testProjectA.id)
        val eitherTags = Await.result(result, Duration.Inf)
        val \/-(tags) = eitherTags

        tags.size should be(tagsList.size)
        tagsList.foreach {
          case (key, tag: ca.shiftfocus.krispii.core.models.Tag) => {
            tags(key).id should be(tag.id)
            tags(key).name should be(tag.name)
          }
        }
      }

      "return empty Vector() project dont have tags" in {
        val result = tagRepository.listByProjectId(TestValues.testProjectE.id)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }

  "RoleRepository.find" should {
    inSequence {
      "find a single entry by ID" in {

        val testTag = TestValues.testTagA

        val result = tagRepository.find(testTag.name)
        val eitherTag = Await.result(result, Duration.Inf)
        val \/-(tag) = eitherTag

        tag.id should be(testTag.id)
        tag.name should be(testTag.name)
      }

      "return RepositoryError.NoResults if entry wasn't found" in {
        val testTag = TestValues.testTagX
        val result = tagRepository.find(testTag.name)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Tag")))
      }
    }
  }
}
