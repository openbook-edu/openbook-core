package com.shiftfocus.krispii.common.models

import com.shiftfocus.krispii.lib.UUID
import org.joda.time.DateTime

case class Session(
  sessionId: UUID = UUID.random,
  userId: UUID,
  ipAddress: String = "",
  location: String = "",
  userAgent: String = "",
  createdAt: Option[DateTime] = None,
  updatedAt: Option[DateTime] = None
)
