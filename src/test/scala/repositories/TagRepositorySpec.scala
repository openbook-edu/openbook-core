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

//class TagRepositorySpec
//    extends TestEnvironment {
//  val tagRepository = new TagRepositoryPostgres
//  "TagRepository.listPopular" should {
//    inSequence {
//      "list popular tags" in {
//        val tagsList = TreeMap[Int, ca.shiftfocus.krispii.core.models.Tag](
//          0 -> TestValues.testTagA,
//          1 -> TestValues.testTagB,
//          2 -> TestValues.testTagC
//        )
//
//        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
//        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))
//
//        val result = tagRepository.listPopular("en", 3, IndexedSeq("subjectects", "levels"))
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        tags.size should be(tagsList.size)
//        tagsList.foreach {
//          case (key, tag: ca.shiftfocus.krispii.core.models.Tag) => {
//            tags(key).category should be(tag.category)
//            tags(key).name should be(tag.name)
//            tags(key).lang should be(tag.lang)
//          }
//        }
//      }
//    }
//  }
//
//  "TagRepository.create" should {
//    inSequence {
//      "create a new tag" in {
//        val testTag = TestValues.testTagX
//        val result = tagRepository.create(testTag)
//        val eitherTag = Await.result(result, Duration.Inf)
//        val \/-(tag) = eitherTag
//
//        tag.lang should be(testTag.lang)
//        tag.name should be(testTag.name)
//        tag.category should be(testTag.category)
//      }
//      "create a new tag with empty category" in {
//        val testTag = TestValues.testTagX.copy(
//          category = None
//        )
//        val result = tagRepository.create(testTag)
//        val eitherTag = Await.result(result, Duration.Inf)
//        val \/-(tag) = eitherTag
//
//        tag.lang should be(testTag.lang)
//        tag.name should be(testTag.name)
//        tag.category should be(testTag.category)
//      }
//      "create a new tag with empty category if category doesn't exist" in {
//        val testTag = TestValues.testTagX.copy(
//          category = Some("unexisting_category")
//        )
//        val result = tagRepository.create(testTag)
//        val eitherTag = Await.result(result, Duration.Inf)
//        val \/-(tag) = eitherTag
//
//        tag.lang should be(testTag.lang)
//        tag.name should be(testTag.name)
//        tag.category should be(None)
//      }
//      "return unique key conflict if tag already exists" in {
//        val result = tagRepository.create(TestValues.testTagA)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
//      }
//    }
//  }
//
//  "TagRepository.delete" should {
//    inSequence {
//      "delete the tag if it doesn't have references in other tables" in {
//        val testTag = TestValues.testTagD
//
//        val result = tagRepository.delete(testTag)
//        Await.result(result, Duration.Inf) should be(\/-(testTag))
//      }
//
//      "return an error if a tag is still attached to a project" in {
//        //tag_id,project_tags_tag_id_fkey
//        val result = tagRepository.delete(TestValues.testTagA)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("tag_id", "project_tags_tag_id_fkey")))
//      }
//
//      "return not found if deleting unexisting tag" in {
//        val result = tagRepository.delete(TestValues.testTagX)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Tag")))
//      }
//    }
//  }
//
//  "TagRepository.untag" should {
//    inSequence {
//      "remove a tag from a project" in {
//
//        val result = tagRepository.untag(TestValues.testProjectG.id, TaggableEntities.project, TestValues.testTagA.name, TestValues.testTagA.lang)
//        Await.result(result, Duration.Inf) should be(\/-(()))
//      }
//      "return RepositoryError.NoResults if something doesn't exist" in {
//        val testProject = TestValues.testProjectH
//        val testTag = TestValues.testTagX
//
//        val result = tagRepository.untag(testProject.id, TaggableEntities.project, testTag.name, testTag.lang)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("Could not remove the tag")))
//      }
//    }
//  }
//
//  "TagRepository.trigramSearch" should {
//    inSequence {
//      "find the tags given the keyword" in {
//
//        val result = tagRepository.trigramSearch("sed")
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        tags.size should be(1)
//
//      }
//      "find admin tags given the keyword" in {
//        val tagsList = TreeMap[Int, ca.shiftfocus.krispii.core.models.Tag](
//          0 -> TestValues.testTagF
//        )
//
//        val result = tagRepository.trigramSearchAdmin("adm")
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        tags.size should be(1)
//        tagsList.foreach {
//          case (key, tag: ca.shiftfocus.krispii.core.models.Tag) => {
//            tags(key).category should be(tag.category)
//            tags(key).name should be(tag.name)
//            tags(key).lang should be(tag.lang)
//          }
//        }
//      }
//      "find admin tags given the keyword for a tagged user" in {
//        val userId = TestValues.testUserA.id
//        val tagsList = TreeMap[Int, ca.shiftfocus.krispii.core.models.Tag](
//          0 -> TestValues.testTagF
//        )
//
//        val result = tagRepository.trigramSearchAdmin("adm", userId)
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        tags.size should be(1)
//        tagsList.foreach {
//          case (key, tag: ca.shiftfocus.krispii.core.models.Tag) => {
//            tags(key).category should be(tag.category)
//            tags(key).name should be(tag.name)
//            tags(key).lang should be(tag.lang)
//          }
//        }
//      }
//      "return no results if the keyword doesn't match anything" in {
//        val result = tagRepository.trigramSearch("coco")
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        tags.size should be(0)
//      }
//      "return no results if the keyword is from admin tag" in {
//        val result = tagRepository.trigramSearch("adm")
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        tags.size should be(0)
//      }
//      "return no results if the keyword is from admin tag and user is not tagged with admin tag" in {
//        val userId = TestValues.testUserB.id
//
//        val result = tagRepository.trigramSearchAdmin("adm", userId)
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        tags.size should be(0)
//      }
//    }
//  }
//
//  "TagRepository.listByProject" should {
//    inSequence {
//      "list tags for project" in {
//        val tagsList = TreeMap[Int, ca.shiftfocus.krispii.core.models.Tag](
//          0 -> TestValues.testTagA,
//          1 -> TestValues.testTagB
//        )
//
//        val result = tagRepository.listByEntity(TestValues.testProjectG.id, TaggableEntities.project)
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        tags.size should be(tagsList.size)
//        tagsList.foreach {
//          case (key, tag: ca.shiftfocus.krispii.core.models.Tag) => {
//            tags(key).category should be(tag.category)
//            tags(key).name should be(tag.name)
//            tags(key).lang should be(tag.lang)
//          }
//        }
//      }
//      "list organizational tags by project" in {
//        val testProject = TestValues.testProjectB
//        val tagsList = TreeMap[Int, ca.shiftfocus.krispii.core.models.Tag](
//          0 -> TestValues.testTagA
//        )
//
//        val result = tagRepository.listOrganizationalByEntity(testProject.id, TaggableEntities.project)
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        println(Console.GREEN + "tags = " + tags + Console.RESET)
//
//        tags.size should be(tagsList.size)
//        tagsList.foreach {
//          case (key, tag: ca.shiftfocus.krispii.core.models.Tag) => {
//            tags(key).category should be(tag.category)
//            tags(key).name should be(tag.name)
//            tags(key).lang should be(tag.lang)
//          }
//        }
//      }
//      "return empty Vector() project dont have tags" in {
//        val result = tagRepository.listByEntity(TestValues.testProjectE.id, TaggableEntities.project)
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//    }
//  }
//
//  "TagRepository.isOrganizational" should {
//    inSequence {
//      "be organizational tag" in {
//        val testTag = TestValues.testTagA
//
//        val result = tagRepository.isOrganizational(testTag.name, testTag.lang)
//        val eitherIsOrg = Await.result(result, Duration.Inf)
//        val \/-(isOrg) = eitherIsOrg
//
//        isOrg should be(true)
//      }
//      "not be organizational tag" in {
//        val testTag = TestValues.testTagC
//
//        val result = tagRepository.isOrganizational(testTag.name, testTag.lang)
//        val eitherIsOrg = Await.result(result, Duration.Inf)
//        val \/-(isOrg) = eitherIsOrg
//
//        isOrg should be(false)
//      }
//      "not be organizational if tag not found" in {
//        val testTag = TestValues.testTagX
//
//        val result = tagRepository.isOrganizational(testTag.name, testTag.lang)
//        val eitherIsOrg = Await.result(result, Duration.Inf)
//        val \/-(isOrg) = eitherIsOrg
//
//        isOrg should be(false)
//      }
//    }
//  }
//
//  "TagRepository.find" should {
//    inSequence {
//      "find a single entry" in {
//
//        val testTag = TestValues.testTagA
//
//        val result = tagRepository.find(testTag.name, testTag.lang)
//        val eitherTag = Await.result(result, Duration.Inf)
//        val \/-(tag) = eitherTag
//
//        tag.lang should be(testTag.lang)
//        tag.name should be(testTag.name)
//        tag.category should be(testTag.category)
//      }
//      "find a single entry without category" in {
//
//        val testTag = TestValues.testTagE
//
//        val result = tagRepository.find(testTag.name, testTag.lang)
//        val eitherTag = Await.result(result, Duration.Inf)
//        val \/-(tag) = eitherTag
//
//        tag.lang should be(testTag.lang)
//        tag.name should be(testTag.name)
//        tag.category should be(testTag.category)
//      }
//      "return RepositoryError.NoResults if entry wasn't found" in {
//        val testTag = TestValues.testTagX
//        val result = tagRepository.find(testTag.name, testTag.lang)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Tag")))
//      }
//    }
//  }
//
//  "TagRepository.update" should {
//    inSequence {
//      "update a single entry" in {
//        val testTag = TestValues.testTagA.copy(
//          name = "New name"
//        )
//
//        val result = tagRepository.update(testTag)
//        val eitherTag = Await.result(result, Duration.Inf)
//        val \/-(tag) = eitherTag
//
//        tag.lang should be(testTag.lang)
//        tag.name should be(testTag.name)
//        tag.category should be(testTag.category)
//      }
//      "update a single entry without category" in {
//        val testTag = TestValues.testTagE.copy(
//          name = "New name"
//        )
//
//        val result = tagRepository.update(testTag)
//        val eitherTag = Await.result(result, Duration.Inf)
//        val \/-(tag) = eitherTag
//
//        tag.lang should be(testTag.lang)
//        tag.name should be(testTag.name)
//        tag.category should be(testTag.category)
//      }
//      "return RepositoryError.NoResults if entry wasn't found" in {
//        val testTag = TestValues.testTagX
//        val result = tagRepository.find(testTag.name, testTag.lang)
//        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Tag")))
//      }
//    }
//  }
//
//  "TagRepository.listByCategory" should {
//    inSequence {
//      "list tags for a given category" in {
//        val tagsList = TreeMap[Int, ca.shiftfocus.krispii.core.models.Tag](
//          0 -> TestValues.testTagB,
//          1 -> TestValues.testTagC,
//          2 -> TestValues.testTagD
//        )
//
//        val result = tagRepository.listByCategory("level", "fr")
//        val eitherTags = Await.result(result, Duration.Inf)
//        val \/-(tags) = eitherTags
//
//        tags.size should be(tagsList.size)
//        tagsList.foreach {
//          case (key, tag: ca.shiftfocus.krispii.core.models.Tag) => {
//            tags(key).category should be(tag.category)
//            tags(key).name should be(tag.name)
//            tags(key).lang should be(tag.lang)
//          }
//        }
//      }
//
//      "return empty Vector() if category dont have tags" in {
//        val result = tagRepository.listByCategory("school", "en")
//        Await.result(result, Duration.Inf) should be(\/-(Vector()))
//      }
//    }
//  }
//}
