import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.repositories.TaskRepositoryPostgres
import org.scalatest.Matchers._
import org.scalatest._
import play.api.Logger

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scalaz._

class TaskRepositorySpec
    extends TestEnvironment {
  val taskRepository = new TaskRepositoryPostgres

  "TaskRepository.list" should {
    inSequence {
      "list all tasks" in {
        // Should be ordered by Position.
        val testTaskList = IndexedSeq[Task](
          TestValues.testLongAnswerTaskA,
          TestValues.testLongAnswerTaskN,
          TestValues.testLongAnswerTaskO,
          TestValues.testShortAnswerTaskB,
          TestValues.testMultipleChoiceTaskC,
          TestValues.testOrderingTaskD,
          TestValues.testMatchingTaskE,
          TestValues.testMatchingTaskK,
          TestValues.testMatchingTaskM,
          TestValues.testOrderingTaskL,
          TestValues.testOrderingTaskN,
          TestValues.testBlanksTaskP,
          TestValues.testMediaTaskA
        )

        val result = taskRepository.list
        val eitherTasks = Await.result(result, Duration.Inf)
        val \/-(tasks) = eitherTasks

        tasks.size should be(testTaskList.size)

        tasks.foreach { task =>
          var testTask = if (testTaskList.exists(_.id == task.id)) testTaskList.filter(_.id == task.id).head
          else fail("There is no task with such ID in testTaskList: " + task.id)

          testTask.version should be(task.version)
          testTask.partId should be(task.partId)
          testTask.taskType should be(task.taskType)
          testTask.position should be(task.position)
          testTask.settings.toString should be(task.settings.toString)
          testTask.createdAt.toString should be(task.createdAt.toString)
          testTask.updatedAt.toString should be(task.updatedAt.toString)

          //Specific
          testTask match {
            case documentTask: DocumentTask => {
              documentTask.dependencyId should be(task.asInstanceOf[DocumentTask].dependencyId)
            }
            case questionTask: QuestionTask => {
              questionTask.questions.toString should be(task.asInstanceOf[QuestionTask].questions.toString)
            }
            case _ => throw new Exception("Invalid task type.")
          }
        }
      }
      "lind all tasks belonging to a given part" in {
        val testPart = TestValues.testPartB

        val testTaskList = IndexedSeq[Task](
          TestValues.testOrderingTaskD,
          TestValues.testMatchingTaskK,
          TestValues.testOrderingTaskL,
          TestValues.testMatchingTaskM
        )

        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val result = taskRepository.list(testPart)
        val eitherTasks = Await.result(result, Duration.Inf)
        val \/-(tasks) = eitherTasks

        tasks.size should be(testTaskList.size)

        tasks.foreach { task =>
          var testTask = if (testTaskList.exists(_.id == task.id)) testTaskList.filter(_.id == task.id).head
          else fail("There is no task with such ID in testTaskList: " + task.id)

          testTask.version should be(task.version)
          testTask.partId should be(task.partId)
          testTask.taskType should be(task.taskType)
          testTask.position should be(task.position)
          testTask.settings.toString should be(task.settings.toString)
          testTask.createdAt.toString should be(task.createdAt.toString)
          testTask.updatedAt.toString should be(task.updatedAt.toString)

          //Specific
          testTask match {
            case documentTask: DocumentTask => {
              documentTask.dependencyId should be(task.asInstanceOf[DocumentTask].dependencyId)
            }
            case questionTask: QuestionTask => {
              questionTask.questions.toString should be(task.asInstanceOf[QuestionTask].questions.toString)
            }
            case _ => throw new Exception("Invalid task type.")
          }
        }
      }
      "return empty Vector() if part doesn't exist" in {
        val testPart = TestValues.testPartD

        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val result = taskRepository.list(testPart)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "list all tasks belonging to a given project" in {
        val testProject = TestValues.testProjectA

        val testTaskList = IndexedSeq[Task](
          TestValues.testLongAnswerTaskA,
          TestValues.testLongAnswerTaskN,
          TestValues.testLongAnswerTaskO,
          TestValues.testShortAnswerTaskB,
          TestValues.testMultipleChoiceTaskC,
          TestValues.testOrderingTaskD,
          TestValues.testMatchingTaskK,
          TestValues.testMatchingTaskM,
          TestValues.testOrderingTaskL,
          TestValues.testOrderingTaskN,
          TestValues.testBlanksTaskP,
          TestValues.testMediaTaskA
        )

        val result = taskRepository.list(testProject)
        val eitherTasks = Await.result(result, Duration.Inf)
        val \/-(tasks) = eitherTasks

        tasks.size should be(testTaskList.size)

        tasks.foreach { task =>
          var testTask = if (testTaskList.exists(_.id == task.id)) testTaskList.filter(_.id == task.id).head
          else fail("There is no task with such ID in testTaskList: " + task.id)

          testTask.version should be(task.version)
          testTask.partId should be(task.partId)
          testTask.taskType should be(task.taskType)
          testTask.position should be(task.position)
          testTask.settings.toString should be(task.settings.toString)
          testTask.createdAt.toString should be(task.createdAt.toString)
          testTask.updatedAt.toString should be(task.updatedAt.toString)

          //Specific
          testTask match {
            case documentTask: DocumentTask => {
              documentTask.dependencyId should be(task.asInstanceOf[DocumentTask].dependencyId)
            }
            case questionTask: QuestionTask => {
              questionTask.questions.toString should be(task.asInstanceOf[QuestionTask].questions.toString)
            }
            case _ => throw new Exception("Invalid task type.")
          }
        }
      }
      "return empty Vector() if project doesn't exist" in {
        val testProject = TestValues.testProjectD

        val result = taskRepository.list(testProject)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }

  "TaskRepository.find" should {
    inSequence {
      "find a a single entry by ID" in {
        val testTask = TestValues.testShortAnswerTaskB

        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val result = taskRepository.find(testTask.id)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.position should be(testTask.position)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "return RepositoryError.NoResults if tas hasn't been found" in {
        val testTask = TestValues.testLongAnswerTaskF

        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val result = taskRepository.find(testTask.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
      "find a task given its position within a part, its part's position within a project, and its project" in {
        val testProject = TestValues.testProjectA
        val testPart = TestValues.testPartB
        val testTask = TestValues.testOrderingTaskD
        val taskPosition = testTask.position

        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val result = taskRepository.find(testProject, testPart, taskPosition)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.position should be(testTask.position)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "return RepositoryError.NoResults if project is wrong" in {
        val testProject = TestValues.testProjectB
        val testPart = TestValues.testPartB
        val testTask = TestValues.testOrderingTaskD
        val taskPosition = testTask.position

        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val result = taskRepository.find(testProject, testPart, taskPosition)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
      "return RepositoryError.NoResults if part is wrong" in {
        val testProject = TestValues.testProjectA
        val testPart = TestValues.testPartA
        val testTask = TestValues.testOrderingTaskD
        val taskPosition = testTask.position

        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val result = taskRepository.find(testProject, testPart, taskPosition)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
      "return RepositoryError.NoResults if task position is wrong" in {
        val testProject = TestValues.testProjectA
        val testPart = TestValues.testPartB
        val testTask = TestValues.testOrderingTaskD
        val taskPosition = testTask.position + 99

        (cache.getCached(_: String)) when (*) returns (Future.successful(-\/(RepositoryError.NoResults(""))))
        (cache.putCache(_: String)(_: Any, _: Option[Duration])) when (*, *, *) returns (Future.successful(\/-(())))

        val result = taskRepository.find(testProject, testPart, taskPosition)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
    }
  }

  "TaskRepository.findNow" should {
    inSequence {
      "find a task on which user is working on now" in {
        // Should return testShortAnswerTaskB, because testLongAnswerTaskA has work that is completed
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectA
        val testTask = TestValues.testShortAnswerTaskB

        val result = taskRepository.findNow(testUser, testProject)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.position should be(testTask.position)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "find a task on which user is working on now within another project" in {
        val testUser = TestValues.testUserE
        val testProject = TestValues.testProjectB
        val testTask = TestValues.testMatchingTaskE

        val result = taskRepository.findNow(testUser, testProject)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.position should be(testTask.position)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "return RepositoryError.NoResults if user is not connected with project" in {
        val testUser = TestValues.testUserG
        val testProject = TestValues.testProjectB

        val result = taskRepository.findNow(testUser, testProject)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
      "return RepositoryError.NoResults if project doesn't have any task" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectE

        val result = taskRepository.findNow(testUser, testProject)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
      "return RepositoryError.NoResults if part is not enabled" in {
        val testUser = TestValues.testUserC
        val testProject = TestValues.testProjectC

        val result = taskRepository.findNow(testUser, testProject)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
    }
  }

  "TaskRepository.findNowFromAll" should {
    inSequence {
      "find a task from all tasks on which someone is working on now" in {
        val testTask = TestValues.testMatchingTaskE

        val result = taskRepository.findNowFromAll
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.position should be(testTask.position)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
    }
  }

  "TaskRepository.insert" should {
    inSequence {
      "insert new LongAnswer task" in {
        val testTask = TestValues.testLongAnswerTaskF

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.insert(testTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(1L)
        task.partId should be(testTask.partId)
        task.position should be(testTask.position)
        task.taskType should be(testTask.taskType)
        task.settings.help should be(testTask.settings.help)
        task.settings.toString should be(testTask.settings.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "insert new media task" in {
        val testTask = TestValues.testMediaTaskB

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.insert(testTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(1L)
        task.partId should be(testTask.partId)
        task.position should be(testTask.position)
        task.taskType should be(testTask.taskType)
        task.settings.help should be(testTask.settings.help)
        task.settings.toString should be(testTask.settings.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case mediaTask: MediaTask => {
            mediaTask.mediaType should be(testTask.asInstanceOf[MediaTask].mediaType)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "return RepositoryError.PrimaryKeyConflict if LongAnswer task already exists" in {
        val testTask = TestValues.testLongAnswerTaskA

        val result = taskRepository.insert(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "insert new ShortAnswer task" in {
        val testTask = TestValues.testShortAnswerTaskG

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.insert(testTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(1L)
        task.partId should be(testTask.partId)
        task.position should be(testTask.position)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "return RepositoryError.PrimaryKeyConflict if ShortAnswer task already exists" in {
        val testTask = TestValues.testShortAnswerTaskB

        val result = taskRepository.insert(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "insert new MultipleChoice task" in {
        val testTask = TestValues.testMultipleChoiceTaskH

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.insert(testTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(1L)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.position should be(testTask.position)
        task.settings.toString should be(testTask.settings.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "return RepositoryError.PrimaryKeyConflict if MultipleChoice task already exists" in {
        val testTask = TestValues.testMultipleChoiceTaskC

        val result = taskRepository.insert(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "insert new Ordering task" in {
        val testTask = TestValues.testOrderingTaskI

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.insert(testTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(1L)
        task.partId should be(testTask.partId)
        task.position should be(testTask.position)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "return RepositoryError.PrimaryKeyConflict if Ordering task already exists" in {
        val testTask = TestValues.testOrderingTaskD

        val result = taskRepository.insert(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "insert new Matching task" in {
        val testTask = TestValues.testMatchingTaskJ

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.insert(testTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(1L)
        task.partId should be(testTask.partId)
        task.position should be(testTask.position)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "return RepositoryError.PrimaryKeyConflict if Matching task already exists" in {
        val testTask = TestValues.testMatchingTaskE

        val result = taskRepository.insert(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
      "insert new Blanks task" in {
        val testTask = TestValues.testBlanksTaskQ

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.insert(testTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(1L)
        task.partId should be(testTask.partId)
        task.position should be(testTask.position)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "return RepositoryError.PrimaryKeyConflict if Blanks task already exists" in {
        val testTask = TestValues.testBlanksTaskP

        val result = taskRepository.insert(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.PrimaryKeyConflict))
      }
    }
  }

  "TaskRepository.update" should {
    inSequence {
      "update Media task" in {
        val testTask = TestValues.testMediaTaskA
        val updatedTask = testTask.copy(
          position = testTask.position + 1,
          settings = testTask.settings.copy(
            title = "updated" + testTask.settings.title,
            description = "updated" + testTask.settings.description,
            notesAllowed = !testTask.settings.notesAllowed,
            notesTitle = Some("updated notes title"),
            help = "updated help info",
            responseTitle = Some("updated response title")
          )
        )

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.update(updatedTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(updatedTask.id)
        task.version should be(updatedTask.version + 1)
        task.partId should be(updatedTask.partId)
        task.taskType should be(updatedTask.taskType)
        task.position should be(updatedTask.position)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(updatedTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(updatedTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case mediaTask: MediaTask => {
            mediaTask.mediaType.toString should be(updatedTask.asInstanceOf[MediaTask].mediaType.toString)
          }

          case _ => throw new Exception("Invalid task type.")
        }
      }

      "update LongAnswer task" in {
        val testTask = TestValues.testLongAnswerTaskA
        val updatedTask = testTask.copy(
          position = testTask.position + 1,
          settings = testTask.settings.copy(
            title = "updated" + testTask.settings.title,
            description = "updated" + testTask.settings.description,
            notesAllowed = !testTask.settings.notesAllowed,
            notesTitle = Some("updated notes title"),
            help = "updated help info",
            responseTitle = Some("updated response title")
          )
        )

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.update(updatedTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(updatedTask.id)
        task.version should be(updatedTask.version + 1)
        task.partId should be(updatedTask.partId)
        task.taskType should be(updatedTask.taskType)
        task.position should be(updatedTask.position)
        task.settings.toString should be(updatedTask.settings.toString)
        task.createdAt.toString should be(updatedTask.createdAt.toString)
        task.updatedAt.toString should not be (updatedTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(updatedTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(updatedTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "update ShortAnswer task" in {
        val testTask = TestValues.testShortAnswerTaskB
        val updatedTask = testTask.copy(
          position = testTask.position + 1,
          settings = testTask.settings.copy(
            title = "updated task title",
            description = "udated task description",
            notesAllowed = false,
            notesTitle = Some("updated notes title"),
            responseTitle = Some("updated response title")
          ),
          // Specific
          questions = testTask.questions :+ TestValues.testOrderingQuestionC
        )

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.update(updatedTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(updatedTask.id)
        task.version should be(updatedTask.version + 1)
        task.partId should be(updatedTask.partId)
        task.taskType should be(updatedTask.taskType)
        task.position should be(updatedTask.position)
        task.settings.toString should be(updatedTask.settings.toString)
        task.createdAt.toString should be(updatedTask.createdAt.toString)
        task.updatedAt.toString should not be (updatedTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(updatedTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(updatedTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "update MultipleChoice task" in {
        val testTask = TestValues.testMultipleChoiceTaskC
        val updatedTask = testTask.copy(
          position = testTask.position + 1,
          settings = testTask.settings.copy(
            title = "updated task title",
            description = "udated task description",
            notesAllowed = false,
            notesTitle = Some("updated notes title"),
            responseTitle = Some("updated response title")
          ),
          // Specific
          questions = testTask.questions :+ TestValues.testOrderingQuestionC
        )

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.update(updatedTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(updatedTask.id)
        task.version should be(updatedTask.version + 1)
        task.partId should be(updatedTask.partId)
        task.taskType should be(updatedTask.taskType)
        task.position should be(updatedTask.position)
        task.settings.toString should be(updatedTask.settings.toString)
        task.createdAt.toString should be(updatedTask.createdAt.toString)
        task.updatedAt.toString should not be (updatedTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(updatedTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(updatedTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "update Ordering task" in {
        val testTask = TestValues.testOrderingTaskD
        val updatedTask = testTask.copy(
          position = testTask.position + 1,
          settings = testTask.settings.copy(
            title = "updated task title",
            description = "udated task description",
            notesAllowed = false,
            notesTitle = Some("updated notes title"),
            responseTitle = Some("updated response title")
          ),
          // Specific
          questions = testTask.questions :+ TestValues.testMatchingQuestionD
        )

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.update(updatedTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(updatedTask.id)
        task.version should be(updatedTask.version + 1)
        task.partId should be(updatedTask.partId)
        task.taskType should be(updatedTask.taskType)
        task.position should be(updatedTask.position)
        task.settings.toString should be(updatedTask.settings.toString)
        task.createdAt.toString should be(updatedTask.createdAt.toString)
        task.updatedAt.toString should not be (updatedTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(updatedTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(updatedTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "update Matching task" in {
        val testTask = TestValues.testMatchingTaskE
        val updatedTask = testTask.copy(
          position = testTask.position + 1,
          settings = testTask.settings.copy(
            title = "updated task title",
            description = "udated task description",
            notesAllowed = false,
            notesTitle = Some("updated notes title"),
            responseTitle = Some("updated response title")
          ),
          // Specific
          questions = testTask.questions :+ TestValues.testOrderingQuestionC
        )

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.update(updatedTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(updatedTask.id)
        task.version should be(updatedTask.version + 1)
        task.partId should be(updatedTask.partId)
        task.taskType should be(updatedTask.taskType)
        task.position should be(updatedTask.position)
        task.settings.toString should be(updatedTask.settings.toString)
        task.createdAt.toString should be(updatedTask.createdAt.toString)
        task.updatedAt.toString should not be (updatedTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(updatedTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(updatedTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "update Blanks task" in {
        val testTask = TestValues.testBlanksTaskP
        val updatedTask = testTask.copy(
          position = testTask.position + 1,
          settings = testTask.settings.copy(
            title = "updated task title",
            description = "udated task description",
            notesAllowed = false,
            notesTitle = Some("updated notes title"),
            responseTitle = Some("updated response title")
          ),
          // Specific
          questions = testTask.questions :+ TestValues.testOrderingQuestionC
        )

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.update(updatedTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(updatedTask.id)
        task.version should be(updatedTask.version + 1)
        task.partId should be(updatedTask.partId)
        task.taskType should be(updatedTask.taskType)
        task.position should be(updatedTask.position)
        task.settings.toString should be(updatedTask.settings.toString)
        task.createdAt.toString should be(updatedTask.createdAt.toString)
        task.updatedAt.toString should not be (updatedTask.updatedAt.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(updatedTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(updatedTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "reutrn RepositoryError.NoResults when update an existing Task with wrong version" in {
        val testTask = TestValues.testMatchingTaskE
        val updatedTask = testTask.copy(
          version = 99L,
          position = testTask.position + 1,
          settings = testTask.settings.copy(
            title = "updated task title",
            description = "udated task description",
            notesAllowed = false,
            notesTitle = Some("updated notes title"),
            responseTitle = Some("updated response title")
          ),
          // Specific
          questions = testTask.questions
        )

        val result = taskRepository.update(updatedTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
      "reutrn RepositoryError.NoResults when update a Task that doesn't exist" in {
        val testTask = TestValues.testMatchingTaskJ
        val updatedTask = testTask.copy(
          position = testTask.position + 1,
          settings = testTask.settings.copy(
            title = "updated task title",
            description = "udated task description",
            notesAllowed = false,
            notesTitle = Some("updated notes title"),
            responseTitle = Some("updated response title")
          ),
          // Specific
          questions = testTask.questions
        )

        val result = taskRepository.update(updatedTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
    }
  }

  "TaskRepository.delete" should {
    inSequence {
      "delete a task that doesn't have references in work table" in {
        val testTask = TestValues.testMatchingTaskK

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.delete(testTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.position should be(testTask.position)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }
      "delete a media task that doesn't have references in work table" in {
        val testTask = TestValues.testMediaTaskA

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.delete(testTask)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: Task) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.position should be(testTask.position)
        task.taskType should be(testTask.taskType)

        //Specific
        task match {
          case documentTask: DocumentTask => {
            documentTask.dependencyId should be(testTask.asInstanceOf[DocumentTask].dependencyId)
          }
          case questionTask: QuestionTask => {
            questionTask.questions.toString should be(testTask.asInstanceOf[QuestionTask].questions.toString)
          }
          case mediaTask: MediaTask => {
            mediaTask.mediaType.toString should be(testTask.asInstanceOf[MediaTask].mediaType.toString)
          }
          case _ => throw new Exception("Invalid task type.")
        }
      }

      "return RepositoryError.ForeignKeyConflict if a task has references in task_feedbacks table" in {
        val testTask = TestValues.testLongAnswerTaskA

        val result = taskRepository.delete(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("task_id", "task_feedbacks_task_id_fkey")))
      }
      "return RepositoryError.ForeignKeyConflict if a task has references in work table" in {
        val testTask = TestValues.testLongAnswerTaskN

        val result = taskRepository.delete(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.ForeignKeyConflict("dependency_id", "document_tasks_dependency_id_fkey")))
      }
      "return RepositoryError.NoResults if a task has wrong version" in {
        val testTask = TestValues.testMatchingTaskK.copy(
          version = 99L
        )

        val result = taskRepository.delete(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
      "return RepositoryError.NoResults if task doesn't exist" in {
        val testTask = TestValues.testMatchingTaskJ

        val result = taskRepository.delete(testTask)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults("ResultSet returned no rows. Could not build entity of type Task")))
      }
      "delete all tasks belonging to a part" in {
        val testPart = TestValues.testPartB

        val testTaskList = IndexedSeq[Task](
          TestValues.testOrderingTaskD,
          TestValues.testOrderingTaskL,
          TestValues.testMatchingTaskK,
          TestValues.testMatchingTaskM
        )

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.delete(testPart)
        val eitherTasks = Await.result(result, Duration.Inf)
        val \/-(tasks) = eitherTasks

        tasks.size should be(testTaskList.size)

        tasks.foreach { task =>
          var testTask = if (testTaskList.exists(_.id == task.id)) testTaskList.filter(_.id == task.id).head
          else fail("There is no task with such ID in testTaskList: " + task.id)

          testTask.version should be(task.version)
          testTask.partId should be(task.partId)
          testTask.taskType should be(task.taskType)
          testTask.position should be(task.position)
          testTask.settings.toString should be(task.settings.toString)
          testTask.createdAt.toString should be(task.createdAt.toString)
          testTask.updatedAt.toString should be(task.updatedAt.toString)

          //Specific
          testTask match {
            case documentTask: DocumentTask => {
              documentTask.dependencyId should be(task.asInstanceOf[DocumentTask].dependencyId)
            }
            case questionTask: QuestionTask => {
              questionTask.questions.toString should be(task.asInstanceOf[QuestionTask].questions.toString)
            }
            case _ => throw new Exception("Invalid task type.")
          }
        }
      }
      "return empty Vector() if Part doesn't have Tasks" in {
        val testPart = TestValues.testPartH

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.delete(testPart)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "return empty Vector() if Part doesn't exist" in {
        val testPart = TestValues.testPartD

        (cache.removeCached(_: String)) when (*) returns (Future.successful(\/-(())))

        val result = taskRepository.delete(testPart)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
    }
  }
}
