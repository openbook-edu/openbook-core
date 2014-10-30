package ca.shiftfocus.krispii.core.models.tasks

case class MatchingTask(
  title: String,
  description: String,
  optionsLeft: IndexedSeq[String],
  optionsRight: IndexedSeq[String],
  answer: IndexedSeq[(Int, Int)],
  randomizeOptions: Boolean = true
) extends Task