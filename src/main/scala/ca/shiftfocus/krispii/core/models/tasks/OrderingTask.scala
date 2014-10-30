package ca.shiftfocus.krispii.core.models.tasks

case class OrderingTask(
  title: String,
  description: String,
  options: IndexedSeq[String],
  answer: IndexedSeq[Int],
  randomizeOptions: Boolean = true
)