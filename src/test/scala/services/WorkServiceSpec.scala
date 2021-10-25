//import java.util.UUID
//import ca.shiftfocus.krispii.core.error.ServiceError
//import ca.shiftfocus.krispii.core.lib.ScalaCachePool
//import ca.shiftfocus.krispii.core.models._
//import ca.shiftfocus.krispii.core.models.tasks._
//import ca.shiftfocus.krispii.core.models.work._
//import ca.shiftfocus.krispii.core.repositories._
//import ca.shiftfocus.krispii.core.services._
//import ca.shiftfocus.krispii.core.services.datasource.DB
//import com.github.mauricio.async.db.Connection
//import org.scalatest._
//import Matchers._
//import ca.shiftfocus.krispii.core.models.user.User
//import org.junit.runner.RunWith
//import org.scalatest.junit.JUnitRunner
//import play.api.i18n.MessagesApi
//import play.api.libs.mailer.MailerClient
//
//import scala.concurrent.duration.Duration
//import scala.concurrent.{Await, Future}
//import scalaz.{-\/, \/-}
//
//@RunWith(classOf[JUnitRunner])
//class WorkServiceSpec
//    extends TestEnvironment(writeToDb = false) {
//  val db = stub[DB]
//  val mockConnection = stub[Connection]
//
//  val authService = stub[AuthService]
//  val schoolService = stub[SchoolService]
//  val projectService = stub[ProjectService]
//  val documentService = stub[DocumentService]
//  val componentService = stub[ComponentService]
//  val workRepository = stub[WorkRepository]
//  val documentRepository = stub[DocumentRepository]
//  val taskFeedbackRepository = stub[TaskFeedbackRepository]
//  val taskScratchpadRepository = stub[TaskScratchpadRepository]
//  val projectScratchpadRepository = stub[ProjectScratchpadRepository]
//  val gfileRepository = stub[GfileRepository]
//
//  val userRepository = stub[UserRepository]
//  val roleRepository = stub[RoleRepository]
//  val sessionRepository = stub[SessionRepository]
//  val revisionRepository = stub[RevisionRepository]
//  val courseRepository = stub[CourseRepository]
//  val projectRepository = stub[ProjectRepository]
//  val partRepository = stub[PartRepository]
//  val taskRepository = stub[TaskRepository]
//
//  val activationRepository = stub[UserTokenRepository]
//  val componentRepository = stub[ComponentRepository]
//  val mailerClient = stub[MailerClient]
//
//  //val authService = new AuthServiceDefault(db, userRepository, roleRepository, activationRepository, sessionRepository, mailerClient, wordRepository)
//  //val schoolService = stub[SchoolService]
//  //val projectService = stub[ProjectService]
//  //val documentService = new DocumentServiceDefault(db, userRepository, documentRepository, revisionRepository)
//  //val componentService = new ComponentServiceDefault(db, authService, projectService, schoolService, componentRepository, userRepository)
//  val workService = new WorkServiceDefault(db, authService, schoolService, projectService, documentService, componentService,
//    workRepository, documentRepository, taskFeedbackRepository, taskScratchpadRepository, projectScratchpadRepository, gfileRepository) {
//    override implicit def conn: Connection = mockConnection
//
//    override def transactional[A](f: Connection => Future[A]): Future[A] = {
//      f(mockConnection)
//    }
//  }
//
//  "WorkService.updateDocumentWork" should {
//    inSequence {
//      "update Document Work" in {
//        val testUser = TestValues.testUserE
//        val testRoleList = IndexedSeq()
//        val testTask = TestValues.testLongAnswerTaskA
//        val testWork = TestValues.testLongAnswerWorkF
//        val updatedWork = testWork.copy(
//          isComplete = !testWork.isComplete
//        )
//
//        (roleRepository.list(_: User)(_: Connection)) when (testUser, *) returns (Future.successful(\/-(testRoleList)))
//        (userRepository.find(_: UUID)(_: Connection)) when (testUser.id, *) returns (Future.successful(\/-(testUser)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.find(_: User, _: Task)(_: Connection)) when (testUser, testTask, *) returns (Future.successful(\/-(testWork)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateDocumentWork(testUser.id, testTask.id, updatedWork.isComplete)
//        val eitherWork = Await.result(result, Duration.Inf)
//        val \/-(work) = eitherWork
//
//        work.id should be(updatedWork.id)
//        work.studentId should be(updatedWork.studentId)
//        work.taskId should be(updatedWork.taskId)
//        work.version should be(updatedWork.version)
//        work.response should be(updatedWork.response)
//        work.isComplete should be(updatedWork.isComplete)
//        work.createdAt.toString should be(updatedWork.createdAt.toString)
//        work.updatedAt.toString should be(updatedWork.updatedAt.toString)
//      }
//      "return ServiceError.BadInput if wrong work type was found" in {
//        val testUser = TestValues.testUserE
//        val testRoleList = IndexedSeq()
//        val testTask = TestValues.testLongAnswerTaskA
//        val testWork = TestValues.testMultipleChoiceWorkH
//        val updatedWork = testWork.copy(
//          isComplete = !testWork.isComplete
//        )
//
//        (roleRepository.list(_: User)(_: Connection)) when (testUser, *) returns (Future.successful(\/-(testRoleList)))
//        (userRepository.find(_: UUID)(_: Connection)) when (testUser.id, *) returns (Future.successful(\/-(testUser)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.find(_: User, _: Task)(_: Connection)) when (testUser, testTask, *) returns (Future.successful(\/-(testWork)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateDocumentWork(testUser.id, testTask.id, updatedWork.isComplete)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("Attempted to update the answer for a question work")))
//      }
//    }
//  }
//
//  "WorkService.updateQuestionWork" should {
//    inSequence {
//      "update Question Work" in {
//        val testUser = TestValues.testUserE
//        val testRoleList = IndexedSeq()
//        val testTask = TestValues.testMultipleChoiceTaskC
//        val testWork = TestValues.testMultipleChoiceWorkH
//        val updatedWork = testWork.copy(
//          response = Answers(Map(
//            TestValues.testMatchingQuestionD.id -> TestValues.testMatchingAnswerD,
//            TestValues.testOrderingQuestionC.id -> TestValues.testOrderingAnswerC
//          )),
//          isComplete = !testWork.isComplete
//        )
//
//        (roleRepository.list(_: User)(_: Connection)) when (testUser, *) returns (Future.successful(\/-(testRoleList)))
//        (userRepository.find(_: UUID)(_: Connection)) when (testUser.id, *) returns (Future.successful(\/-(testUser)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.find(_: User, _: Task)(_: Connection)) when (testUser, testTask, *) returns (Future.successful(\/-(testWork)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateQuestionWork(testUser.id, testTask.id, updatedWork.version, Some(updatedWork.response), Some(updatedWork.isComplete))
//        val eitherWork = Await.result(result, Duration.Inf)
//        val \/-(work) = eitherWork
//
//        work.id should be(updatedWork.id)
//        work.studentId should be(updatedWork.studentId)
//        work.taskId should be(updatedWork.taskId)
//        work.version should be(updatedWork.version)
//        work.response should be(updatedWork.response)
//        work.isComplete should be(updatedWork.isComplete)
//        work.createdAt.toString should be(updatedWork.createdAt.toString)
//        work.updatedAt.toString should be(updatedWork.updatedAt.toString)
//      }
//      "NOT update Question Work without parameters" in {
//        val testUser = TestValues.testUserE
//        val testRoleList = IndexedSeq()
//        val testTask = TestValues.testMultipleChoiceTaskC
//        val testWork = TestValues.testMultipleChoiceWorkH
//        val updatedWork = testWork.copy()
//
//        (roleRepository.list(_: User)(_: Connection)) when (testUser, *) returns (Future.successful(\/-(testRoleList)))
//        (userRepository.find(_: UUID)(_: Connection)) when (testUser.id, *) returns (Future.successful(\/-(testUser)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.find(_: User, _: Task)(_: Connection)) when (testUser, testTask, *) returns (Future.successful(\/-(testWork)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateQuestionWork(testUser.id, testTask.id, updatedWork.version)
//        val eitherWork = Await.result(result, Duration.Inf)
//        val \/-(work) = eitherWork
//
//        work.id should be(updatedWork.id)
//        work.studentId should be(updatedWork.studentId)
//        work.taskId should be(updatedWork.taskId)
//        work.version should be(updatedWork.version)
//        work.response should be(updatedWork.response)
//        work.isComplete should be(updatedWork.isComplete)
//        work.createdAt.toString should be(updatedWork.createdAt.toString)
//        work.updatedAt.toString should be(updatedWork.updatedAt.toString)
//      }
//      "return ServiceError.BadInput if wrong work type was found" in {
//        val testUser = TestValues.testUserE
//        val testRoleList = IndexedSeq()
//        val testTask = TestValues.testMultipleChoiceTaskC
//        val testDocWork = TestValues.testLongAnswerWorkF
//        val testQuestionWork = TestValues.testMultipleChoiceWorkH
//        val updatedWork = testQuestionWork.copy(
//          response = Answers(Map(
//            TestValues.testMatchingQuestionD.id -> TestValues.testMatchingAnswerD,
//            TestValues.testOrderingQuestionC.id -> TestValues.testOrderingAnswerC
//          )),
//          isComplete = !testQuestionWork.isComplete
//        )
//
//        (roleRepository.list(_: User)(_: Connection)) when (testUser, *) returns (Future.successful(\/-(testRoleList)))
//        (userRepository.find(_: UUID)(_: Connection)) when (testUser.id, *) returns (Future.successful(\/-(testUser)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.find(_: User, _: Task)(_: Connection)) when (testUser, testTask, *) returns (Future.successful(\/-(testDocWork)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateQuestionWork(testUser.id, testTask.id, updatedWork.version, Some(updatedWork.response), Some(updatedWork.isComplete))
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("Attempted to update the answer for a document work")))
//      }
//      "return ServiceError.OfflineLockFail if versions don't match" in {
//        val testUser = TestValues.testUserE
//        val testRoleList = IndexedSeq()
//        val testTask = TestValues.testMultipleChoiceTaskC
//        val testWork = TestValues.testMultipleChoiceWorkH
//        val updatedWork = testWork.copy(
//          version = testWork.version + 1,
//          response = Answers(Map(
//            TestValues.testMatchingQuestionD.id -> TestValues.testMatchingAnswerD,
//            TestValues.testOrderingQuestionC.id -> TestValues.testOrderingAnswerC
//          )),
//          isComplete = !testWork.isComplete
//        )
//
//        (roleRepository.list(_: User)(_: Connection)) when (testUser, *) returns (Future.successful(\/-(testRoleList)))
//        (userRepository.find(_: UUID)(_: Connection)) when (testUser.id, *) returns (Future.successful(\/-(testUser)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.find(_: User, _: Task)(_: Connection)) when (testUser, testTask, *) returns (Future.successful(\/-(testWork)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateQuestionWork(testUser.id, testTask.id, updatedWork.version, Some(updatedWork.response), Some(updatedWork.isComplete))
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
//      }
//    }
//  }
//
//  "WorkService.updateAnswer" should {
//    inSequence {
//      "update Question Work" in {
//        val testTask = TestValues.testMultipleChoiceTaskC.copy(
//          questions = IndexedSeq(
//            TestValues.testMatchingQuestionD,
//            TestValues.testOrderingQuestionC
//          )
//        )
//        val testWork = TestValues.testMultipleChoiceWorkH.copy(
//          response = Answers(Map(
//            TestValues.testMatchingQuestionD.id -> TestValues.testMatchingAnswerD,
//            TestValues.testOrderingQuestionC.id -> TestValues.testOrderingAnswerC
//          ))
//        )
//        val (questionId, answer) = testWork.response.underlying.head
//        val newAnswer = TestValues.testMatchingAnswerF
//        val updatedWork = testWork.copy(
//          response = Answers(testWork.response.underlying.updated(questionId, newAnswer))
//        )
//
//        (workRepository.find(_: UUID)(_: Connection)) when (testWork.id, *) returns (Future.successful(\/-(testWork)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateAnswer(testWork.id, testWork.version, questionId, newAnswer)
//        val eitherWork = Await.result(result, Duration.Inf)
//        val \/-(work) = eitherWork
//
//        work.id should be(updatedWork.id)
//        work.studentId should be(updatedWork.studentId)
//        work.taskId should be(updatedWork.taskId)
//        work.version should be(updatedWork.version)
//        work.response should be(updatedWork.response)
//        work.isComplete should be(updatedWork.isComplete)
//        work.createdAt.toString should be(updatedWork.createdAt.toString)
//        work.updatedAt.toString should be(updatedWork.updatedAt.toString)
//      }
//      "return ServiceError.BadInput if wrong work type was found" in {
//        val testTask = TestValues.testMultipleChoiceTaskC.copy(
//          questions = IndexedSeq(
//            TestValues.testMatchingQuestionD,
//            TestValues.testOrderingQuestionC
//          )
//        )
//        val testWork = TestValues.testMultipleChoiceWorkH.copy(
//          response = Answers(Map(
//            TestValues.testMatchingQuestionD.id -> TestValues.testMatchingAnswerD,
//            TestValues.testOrderingQuestionC.id -> TestValues.testOrderingAnswerC
//          ))
//        )
//        val testDocWork = TestValues.testLongAnswerWorkA
//        val (questionId, answer) = testWork.response.underlying.head
//        val newAnswer = TestValues.testMatchingAnswerF
//        val updatedWork = testWork.copy(
//          response = Answers(testWork.response.underlying.updated(questionId, newAnswer))
//        )
//
//        (workRepository.find(_: UUID)(_: Connection)) when (testWork.id, *) returns (Future.successful(\/-(testDocWork)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateAnswer(testWork.id, testWork.version, questionId, newAnswer)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("Attempted to update the answer for a document work")))
//      }
//      "return ServiceError.OfflineLockFail if version is wrong" in {
//        val testTask = TestValues.testMultipleChoiceTaskC.copy(
//          questions = IndexedSeq(
//            TestValues.testMatchingQuestionD,
//            TestValues.testOrderingQuestionC
//          )
//        )
//        val testWork = TestValues.testMultipleChoiceWorkH.copy(
//          response = Answers(Map(
//            TestValues.testMatchingQuestionD.id -> TestValues.testMatchingAnswerD,
//            TestValues.testOrderingQuestionC.id -> TestValues.testOrderingAnswerC
//          ))
//        )
//        val (questionId, answer) = testWork.response.underlying.head
//        val newAnswer = TestValues.testMatchingAnswerF
//        val updatedWork = testWork.copy(
//          response = Answers(testWork.response.underlying.updated(questionId, newAnswer))
//        )
//
//        (workRepository.find(_: UUID)(_: Connection)) when (testWork.id, *) returns (Future.successful(\/-(testWork)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateAnswer(testWork.id, testWork.version + 1, questionId, newAnswer)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.OfflineLockFail))
//      }
//      "return ServiceError.BadInput if a QuestionWork points to a DocumentTask" in {
//        val testTask = TestValues.testLongAnswerTaskA
//        val testWork = TestValues.testMultipleChoiceWorkH.copy(
//          taskId = testTask.id,
//          response = Answers(Map(
//            TestValues.testMatchingQuestionD.id -> TestValues.testMatchingAnswerD,
//            TestValues.testOrderingQuestionC.id -> TestValues.testOrderingAnswerC
//          ))
//        )
//        val (questionId, answer) = testWork.response.underlying.head
//        val newAnswer = TestValues.testMatchingAnswerF
//        val updatedWork = testWork.copy(
//          response = Answers(testWork.response.underlying.updated(questionId, newAnswer))
//        )
//
//        (workRepository.find(_: UUID)(_: Connection)) when (testWork.id, *) returns (Future.successful(\/-(testWork)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateAnswer(testWork.id, testWork.version, questionId, newAnswer)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput("Retrieved a QuestionWork that points to a DocumentTask. Kindly curl up into a ball and cry.")))
//      }
//      "return ServiceError.BadInput if question ID wasn't found" in {
//        val testTask = TestValues.testMultipleChoiceTaskC.copy(
//          questions = IndexedSeq(
//            TestValues.testMatchingQuestionD,
//            TestValues.testOrderingQuestionC
//          )
//        )
//        val testWork = TestValues.testMultipleChoiceWorkH.copy(
//          response = Answers(Map(
//            TestValues.testMatchingQuestionD.id -> TestValues.testMatchingAnswerD,
//            TestValues.testOrderingQuestionC.id -> TestValues.testOrderingAnswerC
//          ))
//        )
//        val questionId = UUID.randomUUID()
//        val newAnswer = TestValues.testMatchingAnswerF
//        val updatedWork = testWork.copy(
//          response = Answers(testWork.response.underlying.updated(questionId, newAnswer))
//        )
//
//        (workRepository.find(_: UUID)(_: Connection)) when (testWork.id, *) returns (Future.successful(\/-(testWork)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateAnswer(testWork.id, testWork.version, questionId, newAnswer)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"There is no Question for answer.")))
//      }
//      "return ServiceError.BadInput if answer type is wrong" in {
//        val testTask = TestValues.testMultipleChoiceTaskC.copy(
//          questions = IndexedSeq(
//            TestValues.testMatchingQuestionD,
//            TestValues.testOrderingQuestionC
//          )
//        )
//        val testWork = TestValues.testMultipleChoiceWorkH.copy(
//          response = Answers(Map(
//            TestValues.testMatchingQuestionD.id -> TestValues.testMatchingAnswerD,
//            TestValues.testOrderingQuestionC.id -> TestValues.testOrderingAnswerC
//          ))
//        )
//        val (questionId, answer) = testWork.response.underlying.head
//        val newAnswer = TestValues.testBlanksAnswerE
//        val updatedWork = testWork.copy(
//          response = Answers(testWork.response.underlying.updated(questionId, newAnswer))
//        )
//
//        (workRepository.find(_: UUID)(_: Connection)) when (testWork.id, *) returns (Future.successful(\/-(testWork)))
//        (projectService.findTask(_: UUID)) when (testTask.id) returns (Future.successful(\/-(testTask)))
//        (workRepository.update(_: Work)(_: Connection)) when (updatedWork, *) returns (Future.successful(\/-(updatedWork)))
//
//        val result = workService.updateAnswer(testWork.id, testWork.version, questionId, newAnswer)
//        Await.result(result, Duration.Inf) should be(-\/(ServiceError.BadInput(s"The provided answer does not match the question type")))
//      }
//    }
//  }
//}
//
