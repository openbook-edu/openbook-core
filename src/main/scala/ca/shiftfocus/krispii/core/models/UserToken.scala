package ca.shiftfocus.krispii.core.models

import java.util.UUID
import org.joda.time.DateTime

/**
 * Represents the authentication token assign to a new user when signing up or to a user which requests a password
 */
case class UserToken(
  userId: UUID,
  token: String,
  tokenType: String,
  createdAt: DateTime = new DateTime
)
