package ca.shiftfocus.krispii.core.models

import java.util.UUID

import org.joda.time.DateTime

/**
 * Created by vzaytseva on 23/02/16.
 */
case class Link(
  link: String,
  courseId: UUID,
  createdAt: DateTime = new DateTime
) {}
