package com.shiftfocus.krispii.core.models.tasks

case class TaskSettings(
  dependencyId: Option[UUID] = None,
  title: String = "",
  description: String = "",
  notesAllowed: Boolean = true
)
