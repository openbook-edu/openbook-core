package com.shiftfocus.krispii.core.models.tasks

case class MultipleChoiceTask(
  title: String,
  description: String,
  options: IndexedSeq[String],
  answer: IndexedSeq[Int],
  randomizeOptions: Boolean = true
) extends Task