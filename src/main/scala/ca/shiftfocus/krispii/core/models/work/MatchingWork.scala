package  ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.krispii.core.models.tasks.MatchingTask._
import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime

case class MatchingWork(
  id: UUID = UUID.random,
  studentId: UUID,
  taskId: UUID,
  override val version: Long,
  override val response: IndexedSeq[Match],
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime

) extends Work {

  override def toString: String ={
    var result="";

    response.zipWithIndex.foreach{case(e,i)=> result=result+i+" = " +e.left.toString + " + " +e.right.toString +"\n"}
    return result+", ";

  }

}

object MatchingWork {


}
