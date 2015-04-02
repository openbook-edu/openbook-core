package ca.shiftfocus.krispii.core.models.work

import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime

case class MultipleChoiceWork(
  id: UUID = UUID.random,
  studentId: UUID,
  taskId: UUID,
  override val version: Long,
  override val response: IndexedSeq[Int],
  isComplete: Boolean = false,
  createdAt: DateTime = new DateTime,
  updatedAt: DateTime = new DateTime
) extends Work {

  override def toString: String ={
    var result="";
    response.zipWithIndex.foreach{case(e,i)=> result=result+ "Question:"+ i + "Answers:"+e.toString +"; \n"}
    return result+", ";

  }

}


object MultipleChoiceWork {


}
