package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.uuid.UUID
import org.joda.time.DateTime

case class Session(
  id: UUID = UUID.random,
  userId: UUID,
  ipAddress: String = "",
  location: String = "",
  userAgent: String = "",
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)
