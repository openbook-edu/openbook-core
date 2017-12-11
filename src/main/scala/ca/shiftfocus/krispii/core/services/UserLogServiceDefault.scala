package ca.shiftfocus.krispii.core.services

import java.util.UUID

import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.UserLog
import ca.shiftfocus.krispii.core.repositories._
import ca.shiftfocus.krispii.core.services.datasource.DB
import com.github.mauricio.async.db.Connection

import scala.concurrent.Future
import scalaz.\/

class UserLogServiceDefault(
    val db: DB,
    val userLogRepository: UserLogRepository
) extends UserLogService {

  implicit def conn: Connection = db.pool

  def create(userId: UUID, logType: String, data: Option[String]): Future[\/[ErrorUnion#Fail, UserLog]] = {
    userLogRepository.insert(UserLog(
      userId = userId,
      logType = logType,
      data = data
    ))
  }
}