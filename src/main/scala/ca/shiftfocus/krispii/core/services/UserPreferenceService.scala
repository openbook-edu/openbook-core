package ca.shiftfocus.krispii.core.services

import java.util.UUID
import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.UserPreference
import scala.concurrent.Future
import scalaz.\/

trait UserPreferenceService extends Service[ErrorUnion#Fail] {
  def get(userId: UUID, pref: String): Future[\/[ErrorUnion#Fail, UserPreference]]
  def list(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[UserPreference]]]
  def set(userId: UUID, name: String, state: String): Future[\/[ErrorUnion#Fail, UserPreference]]
  def delete(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[UserPreference]]]
}
