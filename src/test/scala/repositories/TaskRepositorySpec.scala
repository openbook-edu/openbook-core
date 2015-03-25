import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.repositories.TaskRepositoryPostgres
import scala.collection.immutable.TreeMap
import ca.shiftfocus.krispii.core.models.tasks._
import ca.shiftfocus.krispii.core.models._
import org.scalatest._
import Matchers._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._

class TaskRepositorySpec
  extends TestEnvironment {
  val taskRepository = new TaskRepositoryPostgres

  "TaskRepository.list" should {
    inSequence {
      "list all tasks" in {
        val testTaskList = TreeMap[Int, Task](
          0 -> TestValues.testLongAnswerTaskA,
          1 -> TestValues.testShortAnswerTaskB,
          2 -> TestValues.testMultipleChoiceTaskC,
          3 -> TestValues.testOrderingTaskD,
          4 -> TestValues.testMatchingTaskE
        )

        val result = taskRepository.list
        val eitherTasks = Await.result(result, Duration.Inf)
        val \/-(tasks) = eitherTasks

        tasks.size should be(testTaskList.size)

        testTaskList.foreach {
          case (key, task: Task) => {
            // Common fields
            tasks(key).id should be(task.id)
            tasks(key).version should be(task.version)
            tasks(key).partId should be(task.partId)
            tasks(key).taskType should be(task.taskType)
            tasks(key).settings.toString should be(task.settings.toString)
            tasks(key).createdAt.toString should be(task.createdAt.toString)
            tasks(key).updatedAt.toString should be(task.updatedAt.toString)

            tasks(key) match {
              case longAnswer: LongAnswerTask => {
                task match {
                  case task: LongAnswerTask => {
                    longAnswer.id should be(task.id)
                  }
                }
              }
              case shortAnswer: ShortAnswerTask => {
                task match {
                  case task: ShortAnswerTask => {
                    shortAnswer.maxLength should be(task.maxLength)
                  }
                }
              }
              case multipleChoice: MultipleChoiceTask => {
                task match {
                  case task: MultipleChoiceTask => {
                    multipleChoice.choices should be(task.choices)
                    multipleChoice.answers should be(task.answers)
                    multipleChoice.allowMultiple should be(task.allowMultiple)
                    multipleChoice.randomizeChoices should be(task.randomizeChoices)
                  }
                }
              }
              case ordering: OrderingTask => {
                task match {
                  case task: OrderingTask => {
                    ordering.elements should be(task.elements)
                    ordering.answers should be(task.answers)
                    ordering.randomizeChoices should be(task.randomizeChoices)
                  }
                }
              }
              case matching: MatchingTask => {
                task match {
                  case task: MatchingTask => {
                    matching.elementsLeft should be(task.elementsLeft)
                    matching.elementsRight should be(task.elementsRight)
                    matching.answers should be(task.answers)
                    matching.randomizeChoices should be(task.randomizeChoices)
                  }
                }
              }
            }
          }
        }
      }
      "lind all tasks belonging to a given part" in {
        val testPart = TestValues.testPartB

        val testTaskList = TreeMap[Int, Task](
          0 -> TestValues.testOrderingTaskD
        )

        val result = taskRepository.list(testPart)
        val eitherTasks = Await.result(result, Duration.Inf)
        val \/-(tasks) = eitherTasks

        tasks.size should be(testTaskList.size)

        testTaskList.foreach {
          case (key, task: Task) => {
            // Common fields
            tasks(key).id should be(task.id)
            tasks(key).version should be(task.version)
            tasks(key).partId should be(task.partId)
            tasks(key).taskType should be(task.taskType)
            tasks(key).settings.toString should be(task.settings.toString)
            tasks(key).createdAt.toString should be(task.createdAt.toString)
            tasks(key).updatedAt.toString should be(task.updatedAt.toString)

            tasks(key) match {
              case ordering: OrderingTask => {
                task match {
                  case task: OrderingTask => {
                    ordering.elements should be(task.elements)
                    ordering.answers should be(task.answers)
                    ordering.randomizeChoices should be(task.randomizeChoices)
                  }
                }
              }
            }
          }
        }
      }
      "return empty Vector() if part doesn't exist" in {
        val testPart = TestValues.testPartD

        val result = taskRepository.list(testPart)
        Await.result(result, Duration.Inf) should be(\/-(Vector()))
      }
      "list all tasks belonging to a given project" in {
        val testProject = TestValues.testProjectA

        val testTaskList = TreeMap[Int, Task](
          0 -> TestValues.testLongAnswerTaskA,
          1 -> TestValues.testShortAnswerTaskB,
          2 -> TestValues.testMultipleChoiceTaskC,
          3 -> TestValues.testOrderingTaskD
        )

        val result = taskRepository.list(testProject)
        val eitherTasks = Await.result(result, Duration.Inf)
        val \/-(tasks) = eitherTasks

        tasks.size should be(testTaskList.size)

        testTaskList.foreach {
          case (key, task: Task) => {
            // Common fields
            tasks(key).id should be(task.id)
            tasks(key).version should be(task.version)
            tasks(key).partId should be(task.partId)
            tasks(key).taskType should be(task.taskType)
            tasks(key).settings.toString should be(task.settings.toString)
            tasks(key).createdAt.toString should be(task.createdAt.toString)
            tasks(key).updatedAt.toString should be(task.updatedAt.toString)

            tasks(key) match {
              case longAnswer: LongAnswerTask => {
                task match {
                  case task: LongAnswerTask => {
                    longAnswer.id should be(task.id)
                  }
                }
              }
              case shortAnswer: ShortAnswerTask => {
                task match {
                  case task: ShortAnswerTask => {
                    shortAnswer.maxLength should be(task.maxLength)
                  }
                }
              }
              case multipleChoice: MultipleChoiceTask => {
                task match {
                  case task: MultipleChoiceTask => {
                    multipleChoice.choices should be(task.choices)
                    multipleChoice.answers should be(task.answers)
                    multipleChoice.allowMultiple should be(task.allowMultiple)
                    multipleChoice.randomizeChoices should be(task.randomizeChoices)
                  }
                }
              }
              case ordering: OrderingTask => {
                task match {
                  case task: OrderingTask => {
                    ordering.elements should be(task.elements)
                    ordering.answers should be(task.answers)
                    ordering.randomizeChoices should be(task.randomizeChoices)
                  }
                }
              }
            }
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

        val result = taskRepository.find(testTask.id)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: ShortAnswerTask) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        // Specific fields
        task.maxLength should be(testTask.maxLength)
      }
      "return RepositoryError.NoResults if tas hasn't been found" in {
        val testTask = TestValues.testLongAnswerTaskF

        val result = taskRepository.find(testTask.id)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "find a task given its position within a part, its part's position within a project, and its project" in {
        val testProject  = TestValues.testProjectA
        val testPart     = TestValues.testPartB
        val testTask     = TestValues.testOrderingTaskD
        val taskPosition = testTask.position

        val result = taskRepository.find(testProject, testPart, taskPosition)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: OrderingTask) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        // Specific fields
        task.elements should be(testTask.elements)
        task.answers should be(testTask.answers)
        task.randomizeChoices should be(testTask.randomizeChoices)
      }
      "return RepositoryError.NoResults if project is wrong" in {
        val testProject  = TestValues.testProjectB
        val testPart     = TestValues.testPartB
        val testTask     = TestValues.testOrderingTaskD
        val taskPosition = testTask.position

        val result = taskRepository.find(testProject, testPart, taskPosition)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if part is wrong" in {
        val testProject  = TestValues.testProjectA
        val testPart     = TestValues.testPartA
        val testTask     = TestValues.testOrderingTaskD
        val taskPosition = testTask.position

        val result = taskRepository.find(testProject, testPart, taskPosition)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if task position is wrong" in {
        val testProject  = TestValues.testProjectA
        val testPart     = TestValues.testPartB
        val testTask     = TestValues.testOrderingTaskD
        val taskPosition = testTask.position + 1

        val result = taskRepository.find(testProject, testPart, taskPosition)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
    }
  }

  // TODO check if part enabled true and false
  "TaskRepository.findNow" should {
    inSequence {
      "find a task on which user is working on now" in {
        // Should return testShortAnswerTaskB, because testLongAnswerTaskA has work that is completed
        val testUser     = TestValues.testUserC
        val testProject  = TestValues.testProjectA
        val testTask     = TestValues.testShortAnswerTaskB

        val result = taskRepository.findNow(testUser, testProject)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: ShortAnswerTask) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        // Specific fields
        task.maxLength should be(testTask.maxLength)
      }
      "return RepositoryError.NoResults if user is not connected with project" in {
        val testUser     = TestValues.testUserG
        val testProject  = TestValues.testProjectB

        val result = taskRepository.findNow(testUser, testProject)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "return RepositoryError.NoResults if project doesn't have any task" in {
        val testUser     = TestValues.testUserC
        val testProject  = TestValues.testProjectE

        val result = taskRepository.findNow(testUser, testProject)
        Await.result(result, Duration.Inf) should be(-\/(RepositoryError.NoResults))
      }
      "find a task on which user is working on now within a project" in {
        val testUser     = TestValues.testUserC
        val testProject  = TestValues.testProjectB
        val testTask     = TestValues.testMatchingTaskE

        val result = taskRepository.findNow(testUser, testProject)
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: MatchingTask) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        // Specific
        task.elementsLeft should be(testTask.elementsLeft)
        task.elementsRight should be(testTask.elementsRight)
        task.answers should be(testTask.answers)
        task.randomizeChoices should be(testTask.randomizeChoices)
      }
      "find a task from all tasks on which someone is working on now" in {
        val testTask     = TestValues.testShortAnswerTaskB

        val result = taskRepository.findNowFromAll
        val eitherTask = Await.result(result, Duration.Inf)
        val \/-(task: ShortAnswerTask) = eitherTask

        task.id should be(testTask.id)
        task.version should be(testTask.version)
        task.partId should be(testTask.partId)
        task.taskType should be(testTask.taskType)
        task.settings.toString should be(testTask.settings.toString)
        task.createdAt.toString should be(testTask.createdAt.toString)
        task.updatedAt.toString should be(testTask.updatedAt.toString)

        // Specific fields
        task.maxLength should be(testTask.maxLength)
      }
    }
  }
}
