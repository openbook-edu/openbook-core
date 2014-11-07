package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.krispii.core.lib.UUID
import ca.shiftfocus.krispii.core.models.tasks.Task._
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import play.api.libs.functional.syntax._
import play.api.libs.json._
import org.joda.time.DateTime

/**
 * The "work" trait is the supertype for work that students have done.
 */
trait Work {
  val studentId: UUID
  val taskId: UUID
  val sectionId: UUID
  val revision: Long
  val version: Long = 0
  val answer: AnyRef
  val createdAt: Option[DateTime]
  val updatedAt: Option[DateTime]
}

object Work {

  implicit val jsonReads = new Reads[Work] {
    def reads(js: JsValue) = JsSuccess({
      val studentId = (js \ "studentId").as[UUID]
      val taskId    = (js \ "taskId"   ).as[UUID]
      val sectionId = (js \ "sectionId").as[UUID]
      val revision  = (js \ "revision" ).as[Long]
      val answer    = (js \ "answer")
      val createdAt = (js \ "createdAt").as[Option[DateTime]]
      val updatedAt = (js \ "updatedAt").as[Option[DateTime]]

      (js \ "workType").as[Int] match {
        case LongAnswer => LongAnswerWork(studentId = studentId,
                                          taskId = taskId,
                                          sectionId = sectionId,
                                          revision = revision,
                                          answer = answer.as[String],
                                          createdAt = createdAt,
                                          updatedAt = updatedAt)

        case ShortAnswer => ShortAnswerWork(studentId = studentId,
                                            taskId = taskId,
                                            sectionId = sectionId,
                                            revision = revision,
                                            answer = answer.as[String],
                                            createdAt = createdAt,
                                            updatedAt = updatedAt)

        case MultipleChoice => MultipleChoiceWork(studentId = studentId,
                                                  taskId = taskId,
                                                  sectionId = sectionId,
                                                  revision = revision,
                                                  answer = answer.as[IndexedSeq[Int]],
                                                  createdAt = createdAt,
                                                  updatedAt = updatedAt)

        case Ordering => OrderingWork(studentId = studentId,
                                      taskId = taskId,
                                      sectionId = sectionId,
                                      revision = revision,
                                      answer = answer.as[IndexedSeq[Int]],
                                      createdAt = createdAt,
                                      updatedAt = updatedAt)

        case Matching => MatchingWork(studentId = studentId,
                                      taskId = taskId,
                                      sectionId = sectionId,
                                      revision = revision,
                                      answer = answer.as[IndexedSeq[Match]],
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
        "sectionId" -> work.sectionId,
        "revision" -> work.revision,
        "answer" -> {work match {
          case specific: LongAnswerWork => specific.answer
          case specific: ShortAnswerWork => specific.answer
          case specific: MultipleChoiceWork => specific.answer
          case specific: OrderingWork => specific.answer
          case specific: MatchingWork => specific.answer
          case _ => throw new Exception("Tried to serialize a work type that, somehow, doesn't exist.")
        }},
        "createdAt" -> work.createdAt,
        "updatedAt" -> work.updatedAt
      )
  }

}