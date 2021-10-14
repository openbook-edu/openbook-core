//package services
//
//import ca.shiftfocus.krispii.core.repositories._
//import ca.shiftfocus.krispii.core.services.{OmsServiceDefault, OrganizationService, PaymentService, SchoolService}
//import ca.shiftfocus.krispii.core.services.datasource._
//import com.github.mauricio.async.db.Connection
//import org.junit.runner.RunWith
//import org.scalamock.clazz.MockImpl.stub
//import org.scalatest.Matchers
//import org.scalatest.Matchers.convertToAnyShouldWrapper
//import org.scalatest.junit.JUnitRunner
//
//import scala.concurrent.Future
//
//@RunWith(classOf[JUnitRunner])
//class OmsServiceSpec
//    extends TestEnvironment(writeToDb = false) {
//  val db = stub[DB]
//  val mockConnection = stub[Connection]
//
//  val accountRepository = stub[AccountRepository]
//  val chatRepository = stub[ChatRepository]
//  val copiesCountRepository = stub[CopiesCountRepository]
//  val examRepository = stub[ExamRepository]
//  val lastSeenRepository = stub[LastSeenRepository]
//  val limitRepository = stub[LimitRepository]
//  val roleRepository = stub[RoleRepository]
//  val teamRepository = stub[TeamRepository]
//  val testRepository = stub[TestRepository]
//  val scoreRepository = stub[ScoreRepository]
//  val scorerRepository = stub[ScorerRepository]
//  val userRepository = stub[UserRepository]
//  val organizationService = stub[OrganizationService]
//  val paymentService = stub[PaymentService]
//  val schoolService = stub[SchoolService]
//
//  val omsService = new OmsServiceDefault(db, accountRepository, chatRepository, copiesCountRepository, examRepository,
//    lastSeenRepository, limitRepository, roleRepository, teamRepository, testRepository, scoreRepository, scorerRepository,
//    userRepository, organizationService, paymentService, schoolService) {
//    override implicit def conn: Connection = mockConnection
//    override def transactional[A](f: Connection => Future[A]): Future[A] = {
//      f(mockConnection)
//    }
//  }
//
//}