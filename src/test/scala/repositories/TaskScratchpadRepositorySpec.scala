//import ca.shiftfocus.krispii.core.repositories._
//
//class TaskScratchpadRepositorySpec
//  extends TestEnvironment
//{
//  val taskScratchpadRepository = new TaskScratchpadRepositoryPostgres
//
//  "TaskSratchpadRepository.list" should {
//    inSequence {
//      "list a user's latest revisions for each task in a project" in {
//        val testUser = TestValues.testUserC
//        val testProject = TestValues.testProjectA
//
//        val testTaskFeedbackList = TreeMap[Int, TaskFeedback](
//          0 -> TestValues.testTaskFeedbackA,
//          1 -> TestValues.testTaskFeedbackE
//        )
//
//        val testDocumentList = TreeMap[Int, Document](
//          0 -> TestValues.testDocumentF,
//          1 -> TestValues.testDocumentJ
//        )
//
//        testDocumentList.foreach {
//          case (key, document: Document) => {
//            (documentRepository.find(_: UUID, _: Long)(_: Connection)) when(document.id, *, *) returns(Future.successful(\/-(document)))
//          }
//        }
//
//        val result = taskFeedbackRepository.list(testUser, testProject)
//        val eitherTaskFeedbacks = Await.result(result, Duration.Inf)
//        val \/-(taskFeedbacks) = eitherTaskFeedbacks
//
//        taskFeedbacks.size should be(testTaskFeedbackList.size)
//
//        testTaskFeedbackList.foreach {
//          case (key, taskFeedback: TaskFeedback) => {
//            taskFeedbacks(key).studentId should be(taskFeedback.studentId)
//            taskFeedbacks(key).taskId should be(taskFeedback.taskId)
//            taskFeedbacks(key).version should be(taskFeedback.version)
//            taskFeedbacks(key).documentId should be(taskFeedback.documentId)
//            taskFeedbacks(key).createdAt.toString should be(taskFeedback.createdAt.toString)
//            taskFeedbacks(key).updatedAt.toString should be(taskFeedback.updatedAt.toString)
//          }
//        }
//      }
//    }
//  }
//}
