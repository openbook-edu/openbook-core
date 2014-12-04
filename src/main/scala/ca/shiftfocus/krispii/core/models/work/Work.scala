package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import ca.shiftfocus.krispii.core.models.tasks.Task._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import com.github.mauricio.async.db.RowData
import play.api.libs.functional.syntax._
import play.api.libs.json._
import org.joda.time.DateTime

/**
 * The "work" trait is the supertype for work that students have done.
 */
trait Work {
  val studentId: UUID
  val taskId: UUID
  val classId: UUID
  val revision: Long
  val version: Long = 0
  val answer: AnyRef
  val isComplete: Boolean
  val createdAt: Option[DateTime]
  val updatedAt: Option[DateTime]
}

object Work {

  def apply(row: RowData): Work = {
    row("work_type").asInstanceOf[Int] match {
      case LongAnswer => LongAnswerWork(row)
      case ShortAnswer => ShortAnswerWork(row)
      case MultipleChoice => MultipleChoiceWork(row)
      case Ordering => OrderingWork(row)
      case Matching => MatchingWork(row)
      case _ => throw new Exception("Retrieved an unknown task type from the database. You dun messed up now!")
    }
  }

  implicit val jsonReads = new Reads[Work] {
    def reads(js: JsValue) = JsSuccess({
      val studentId  = (js \ "studentId").as[UUID]
      val taskId     = (js \ "taskId"   ).as[UUID]
      val classId  = (js \ "classId").as[UUID]
      val revision   = (js \ "revision" ).as[Long]
      val answer     = (js \ "answer")
      val isComplete = (js \ "isComplete").as[Boolean]
      val createdAt  = (js \ "createdAt").as[Option[DateTime]]
      val updatedAt  = (js \ "updatedAt").as[Option[DateTime]]

      (js \ "workType").as[Int] match {
        case LongAnswer => LongAnswerWork(studentId = studentId,
                                          taskId = taskId,
                                          classId = classId,
                                          revision = revision,
                                          answer = answer.as[String],
                                          isComplete = isComplete,
                                          createdAt = createdAt,
                                          updatedAt = updatedAt)

        case ShortAnswer => ShortAnswerWork(studentId = studentId,
                                            taskId = taskId,
                                            classId = classId,
                                            revision = revision,
                                            answer = answer.as[String],
                                            isComplete = isComplete,
                                            createdAt = createdAt,
                                            updatedAt = updatedAt)

        case MultipleChoice => MultipleChoiceWork(studentId = studentId,
                                                  taskId = taskId,
                                                  classId = classId,
                                                  revision = revision,
                                                  answer = answer.as[IndexedSeq[Int]],
                                                  isComplete = isComplete,
                                                  createdAt = createdAt,
                                                  updatedAt = updatedAt)

        case Ordering => OrderingWork(studentId = studentId,
                                      taskId = taskId,
                                      classId = classId,
                                      revision = revision,
                                      answer = answer.as[IndexedSeq[Int]],
                                      isComplete = isComplete,
                                      createdAt = createdAt,
                                      updatedAt = updatedAt)

        case Matching => MatchingWork(studentId = studentId,
                                      taskId = taskId,
                                      classId = classId,
                                      revision = revision,
                                      answer = answer.as[IndexedSeq[Match]],
                                      isComplete = isComplete,
                                      createdAt = createdAt,
                                      updatedAt = updatedAt)

        case _ => throw new Exception("Tried to unserialize a type of work that doesn't exist.")
      }
    })
  }

  implicit val jsonWrites = new Writes[Work] {
    def writes(work: Work): JsValue =
      Json.obj(
        "studentId" -> work.studentId,
        "taskId" -> work.taskId,
        "classId" -> work.classId,
        "revision" -> work.revision,
        "answer" -> {work match {
          case specific: LongAnswerWork => specific.answer
          case specific: ShortAnswerWork => specific.answer
          case specific: MultipleChoiceWork => specific.answer
          case specific: OrderingWork => specific.answer
          case specific: MatchingWork => specific.answer
          case _ => throw new Exception("Tried to serialize a work type that, somehow, doesn't exist.")
        }},
        "isComplete" -> work.isComplete,
        "createdAt" -> work.createdAt,
        "updatedAt" -> work.updatedAt
      )
  }

}