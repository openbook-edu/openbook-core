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
  extends TestEnvironment
{
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
                   case task: LongAnswerTask =>  {
                     longAnswer.id should be(task.id)
                   }
                 }
               }
               case shortAnswer: ShortAnswerTask => {
                 task match {
                   case task: ShortAnswerTask =>  {
                     shortAnswer.maxLength should be(task.maxLength)
                   }
                 }
               }
               case multipleChoice: MultipleChoiceTask => {
                 console_log(multipleChoice)
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
                   case task: OrderingTask =>  {
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
     }
   }
}
