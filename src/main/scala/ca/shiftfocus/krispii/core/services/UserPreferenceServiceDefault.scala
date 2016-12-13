package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.UserPreference
import ca.shiftfocus.krispii.core.repositories.UserPreferenceRepository
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

class UserPreferenceServiceDefault(
    val db: DB,
    val userPreferenceRepository: UserPreferenceRepository
) extends UserPreferenceService {

  def get(userId: UUID, pref: String): Future[\/[ErrorUnion#Fail, UserPreference]] = {
    transactional { implicit conn: Connection =>
      userPreferenceRepository.get(userId, pref)
    }
  }

  def list(userId: UUID): Future[\/[ErrorUnion#Fail, IndexedSeq[UserPreference]]] = {
    transactional { implicit conn: Connection =>
      userPreferenceRepository.list(userId)
    }
  }

  def set(userId: UUID, name: String, state: String): Future[\/[ErrorUnion#Fail, UserPreference]] = {
    transactional { implicit conn: Connection =>
      userPreferenceRepository.set(UserPreference(
        userId = userId,
        prefName = name,
        state = state
      ))
    }
  }
}
