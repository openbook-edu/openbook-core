package ca.shiftfocus.krispii.core.services.error

sealed trait ServiceError {
  def message: String
}

case class NotFound(message: String) extends ServiceError
case class AlreadyExists(message: String) extends ServiceError
case class BadInput(message: String) extends ServiceError
case class EmailTaken(message: String) extends ServiceError
case class UsernameTaken(message: String) extends ServiceError
case class GenericError(message: String) extends ServiceError
case class UncaughtException(message: String) extends ServiceError