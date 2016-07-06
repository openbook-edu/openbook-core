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
/**
 * Created by vzaytseva on 20/06/16.
 */
class TagCategoryRepositorySpec
    extends TestEnvironment {
  val tagCategoryRepository = new TagCategoryRepositoryPostgres
  "TagCategoryRepository.create" should {
    inSequence {
      "create a new tag category" in {
        val testTagCategory = TestValues.testTagCategoryD
        val result = tagCategoryRepository.create(testTagCategory)
        val eitherTagCategory = Await.result(result, Duration.Inf)
        val \/-(tagCategory) = eitherTagCategory

        tagCategory.name should be(tagCategory.name)
        tagCategory.lang should be(tagCategory.lang)
      }
      "return unique key conflict if tag already exists" in {
        val result = tagCategoryRepository.create(TestValues.testTagCategoryA)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }

  "TagCategoryRepository.delete" should {
    inSequence {
      "delete the tag category if it doesn't have references in other tables" in {
        val result = tagCategoryRepository.delete(TestValues.testTagCategoryA.name)
        Await.result(result, Duration.Inf) should be(\/-(TestValues.testTagCategoryA))
      }

      "return not found if deleting unexisting category" in {
        val result = tagCategoryRepository.delete(TestValues.testTagCategoryD.name)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type TagCategory")))
      }
    }
  }

}