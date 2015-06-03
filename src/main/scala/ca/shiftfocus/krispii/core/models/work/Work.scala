package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.krispii.core.models.document.Document
import ca.shiftfocus.krispii.core.models.tasks.MatchingTask.Match
import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json.{Json, JsValue, Writes}

/**
 * The "work" trait is the supertype for work that students have done.
 *
 * Sealed so that all possible sub-types must be defined here.
 */
sealed trait Work {
  val id: UUID
  val studentId: UUID
  val taskId: UUID
  val version: Long
  val isComplete: Boolean
  val createdAt: DateTime
  val updatedAt: DateTime
  def responseToString:String ={
    "work"
  }
}

object Work {

  implicit val jsonWrites = new Writes[Work] {
    def writes(work: Work): JsValue = {
      val jsVal = Json.obj(
        "id" -> work.id,
        "studentId" -> work.studentId,
        "taskId" -> work.taskId,
        "version" -> work.version,
        "response" -> {
          work match {
            case specific: IntListWork   => specific.response
            case specific: MatchListWork => specific.response
            case specific: DocumentWork  => specific.response
          }
        },
        "isComplete" -> work.isComplete,
        "createdAt" -> work.createdAt,
        "updatedAt" -> work.updatedAt
      )
      work match {
        case docWork: DocumentWork => jsVal + ("documentId" -> Json.toJson(docWork.documentId))
        case _ => jsVal
      }
    }
  }

}

/**
 * DocumentWork are types of work whose storage is backed by the DocumentService and use
 * operational-transformation to synchronize between multiple clients. Eventually all
 * types of work should move to ot-based DocumentWork.
 */
sealed trait DocumentWork extends Work {
  val documentId: UUID
  val response: Option[Document]

  def copy(id: UUID = this.id,
           studentId: UUID = this.studentId,
           taskId: UUID = this.taskId,
           documentId: UUID = this.documentId,
           version: Long = this.version,
           response: Option[Document] = this.response,
           isComplete: Boolean = this.isComplete,
           createdAt: DateTime = this.createdAt,
           updatedAt: DateTime = this.updatedAt
  ) = {
    this match {
      case longAnswerWork: LongAnswerWork   => LongAnswerWork(id, studentId, taskId, documentId, version, response, isComplete, createdAt, updatedAt)
      case shortAnswerWork: ShortAnswerWork => ShortAnswerWork(id, studentId, taskId, documentId, version, response, isComplete, createdAt, updatedAt)
    }
  }
  override def responseToString: String ={
      if(response.isDefined)
      response.get.plaintext
      else
      "Response is empty"
  }
}


case class LongAnswerWork(
  id: UUID = UUID.randomUUID,
  studentId: UUID,
  taskId: UUID,
  documentId: UUID,
  version: Long = 1L,
  response: Option[Document] = None,
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends DocumentWork

case class ShortAnswerWork(
  id: UUID = UUID.randomUUID,
  studentId: UUID,
  taskId: UUID,
  documentId: UUID,
  version: Long = 1L,
  response: Option[Document] = None,
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends DocumentWork


/**
 * ListWork are types of work that store lists of things and map to arrays in the database storage
 * layer.
 *
 * @tparam A the thing that the work is a list of
 */
sealed trait ListWork[A] extends Work {
  val response: IndexedSeq[A]
}
sealed trait IntListWork extends ListWork[Int] {
  override val response: IndexedSeq[Int]
}
sealed trait MatchListWork extends ListWork[Match] {
  override val response: IndexedSeq[Match]
}

case class MultipleChoiceWork(
  id: UUID = UUID.randomUUID,
  studentId: UUID,
  taskId: UUID,
  override val version: Long,
  response: IndexedSeq[Int],
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends IntListWork {

  override def responseToString: String ={
    var result=""
    response.zipWithIndex.foreach{case(e,i)=> result=result+ "Question: "+ i + " Answer: "+e.toString+", "}
    """""""+result.dropRight(2)+"""""""
  }

}

case class OrderingWork(
  id: UUID = UUID.randomUUID,
  studentId: UUID,
  taskId: UUID,
  override val version: Long = 1L,
  response: IndexedSeq[Int],
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends IntListWork {

 override def responseToString: String ={
    response.mkString(" -> ")
  }

}

case class MatchingWork(
   id: UUID = UUID.randomUUID,
   studentId: UUID,
   taskId: UUID,
   override val version: Long,
   response: IndexedSeq[Match],
   isComplete: Boolean = false,
   createdAt: DateTime = new DateTime,
   updatedAt: DateTime = new DateTime
 ) extends MatchListWork {

  override def responseToString: String ={
    var result=""
    response.zipWithIndex.foreach{case(e,i)=> result=result+i+" = " +e.left.toString + " + " +e.right.toString +", "}
    '"' +result.dropRight(2)+'"'

  }
}