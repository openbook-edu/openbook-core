//import ca.shiftfocus.krispii.core.error.{RepositoryError, ServiceError}
//import ca.shiftfocus.krispii.core.lib.ScalaCachePool
//import ca.shiftfocus.krispii.core.models._
//import ca.shiftfocus.krispii.core.models.tasks._
//import ca.shiftfocus.krispii.core.repositories._
//import ca.shiftfocus.krispii.core.services._
//import ca.shiftfocus.krispii.core.services.datasource.DB
//import com.github.mauricio.async.db.Connection
//import java.util.UUID
//
//import org.junit.runner.RunWith
//import org.scalatest.junit.JUnitRunner
////import org.scalatest._
//import org.scalatest.Matchers._
//import play.api.Configuration
//
//import scala.collection.immutable.TreeMap
//import scala.concurrent.duration.Duration
//import scala.concurrent._
//import scalaz.{-\/, \/-}
//import ExecutionContext.Implicits.global
//
//@RunWith(classOf[JUnitRunner])
//class TagServiceSpec
//    extends TestEnvironment(writeToDb = false) {
//
//  val db = stub[DB]
//  val mockConnection = stub[Connection]
//  val tagRepository = stub[TagRepository]
//  val tagCategoryRepository = stub[TagCategoryRepository]
//  val organizationRepository = stub[OrganizationRepository]
//  val limitRepository = stub[LimitRepository]
//  val userRepository = stub[UserRepository]
//  val courseRepository = stub[CourseRepository]
//  val accountRepository = stub[AccountRepository]
//  val stripeRepository = stub[StripeEventRepository]
//  val paymentLogRepository = stub[PaymentLogRepository]
//  val config = stub[Configuration]
//  val projectRepository = stub[ProjectRepository]
//  val roleRepository = stub[RoleRepository]
//  val userPreferenceRepository = stub[UserPreferenceRepository]
//  val paymentService = stub[PaymentService]
//  val trialDays = config.get[Option[Int]]("default.trial.days").get
//  val defaultStudentLimit = config.get[Option[Int]]("default.student.limit").get
//  val defaultStorageLimit = config.get[Option[Int]]("default.storage.limit.gb").get
//  val defaultCourseLimit = config.get[Option[Int]]("default.course.limit").get
//
//  val tagService = new TagServiceDefault(db, tagRepository, tagCategoryRepository, organizationRepository, limitRepository,
//    userRepository, courseRepository, accountRepository, stripeRepository, paymentLogRepository, config,
//    projectRepository, roleRepository, userPreferenceRepository, paymentService) {
//
//    override implicit def conn: Connection = mockConnection
//
//    override def transactional[A](f: Connection => Future[A]): Future[A] = {
//      f(mockConnection)
//    }
//  }
//
//  "ProjectService.cloneTags" should {
//    inSequence {
//      "copy tags from one project to another" in {
//        val toClone = TestValues.testProjectA
//        val cloned = TestValues.testProjectC
//        val tags = TreeMap[Int, Tag](
//          0 -> TestValues.testTagA,
//          1 -> TestValues.testTagB
//        )
//
//        (tagRepository.find(_: UUID)(_: Connection)) when (toClone.id, *) returns (Future.successful(\/-(TestValues.testTagA)))
//        (tagRepository.create(_: Tag)(_: Connection)) when (TestValues.testTagA, *) returns (Future.successful(\/-(TestValues.testTagA)))
//        (tagRepository.create(_: Tag)(_: Connection)) when (TestValues.testTagB, *) returns (Future.successful(\/-(TestValues.testTagB)))
//
//        val result = tagService.cloneTags(cloned.id, toClone.id)
//        val \/-(clonedTags) = Await.result(result, Duration.Inf)
//
//        tags.foreach {
//          case (key, tag: ca.shiftfocus.krispii.core.models.Tag) => {
//            clonedTags(key).lang should be(tag.lang)
//            clonedTags(key).name should be(tag.name)
//            clonedTags(key).category should be(tag.category)
//          }
//        }
//      }
//      "do nothing if there are no tags" in {
//        val toClone = TestValues.testProjectB
//        val cloned = TestValues.testProjectC
//
//        (tagRepository.find(_: UUID)(_: Connection)) when (toClone.id, *) returns (Future.successful(\/-(TestValues.testTagA)))
//        (tagRepository.create(_: ca.shiftfocus.krispii.core.models.Tag)(_: Connection)) when (TestValues.testTagB, *) returns (Future.successful(\/-(TestValues.testTagB)))
//
//        val result = tagService.cloneTags(cloned.id, toClone.id)
//        val \/-(clonedTags) = Await.result(result, Duration.Inf)
//
//        clonedTags should be(Vector())
//      }
//    }
//  }
//}
