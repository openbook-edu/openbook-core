package ca.shiftfocus.krispii.core.models

import java.util.UUID

import play.api.libs.json.{JsValue, Json, Writes}

/**
 * Preferences (id, machine name) should be added manually to a database
 * UserPreferences object is a combination of preferences table and users_preferences table
 *
 * @param userId ID of the user
 * @param prefName Machine name of the existing preference from database preferences.machine_name
 * @param state
 */
case class UserPreference(
  userId: UUID,
  prefName: String,
  state: String
)

object UserPreference {
  implicit val userPreferenceWrites = new Writes[UserPreference] {
    def writes(userPreference: UserPreference): JsValue = {
      Json.obj(
        "userId" -> userPreference.userId.toString,
        "prefName" -> userPreference.prefName,
        "state" -> userPreference.state
      )
    }
  }
}