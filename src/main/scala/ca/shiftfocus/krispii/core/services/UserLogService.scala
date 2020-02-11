package ca.shiftfocus.krispii.core.services

import java.util.UUID
import ca.shiftfocus.krispii.core.error.ErrorUnion
import ca.shiftfocus.krispii.core.models.UserLog
import scala.concurrent.Future
import scalaz.\/

trait UserLogService extends Service[ErrorUnion#Fail] {
  def create(userId: UUID, logType: String, data: Option[String]): Future[\/[ErrorUnion#Fail, UserLog]]
}