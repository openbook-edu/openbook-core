package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime

case class Session(
  id: UUID = UUID.randomUUID,
  userId: UUID,
  ipAddress: String = "",
  location: String = "",
  userAgent: String = "",
  accessToken: Option[String] = None,
  refreshToken: Option[String] = None,
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)
