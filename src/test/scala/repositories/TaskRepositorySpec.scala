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
                    multipleChoice.answer should be(task.answer)
                    multipleChoice.allowMultiple should be(task.allowMultiple)
                    multipleChoice.randomizeChoices should be(task.randomizeChoices)
                  }
                }
              }
              case ordering: OrderingTask => {
                task match {
                  case task: OrderingTask => {
                    ordering.elements should be(task.elements)
                    ordering.answer should be(task.answer)
                    ordering.randomizeChoices should be(task.randomizeChoices)
                  }
                }
              }
              case matching: MatchingTask => {
                task match {
                  case task: MatchingTask => {
                    matching.elementsLeft should be(task.elementsLeft)
                    matching.elementsRight should be(task.elementsRight)
                    matching.answer should be(task.answer)
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
                    ordering.answer should be(task.answer)
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
                    multipleChoice.answer should be(task.answer)
                    multipleChoice.allowMultiple should be(task.allowMultiple)
                    multipleChoice.randomizeChoices should be(task.randomizeChoices)
                  }
                }
              }
              case ordering: OrderingTask => {
                task match {
                  case task: OrderingTask => {
                    ordering.elements should be(task.elements)
                    ordering.answer should be(task.answer)
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
}
