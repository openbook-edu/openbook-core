package ca.shiftfocus.krispii.core.repositories.error

sealed trait RepositoryError {
  def message: String
}

case class NoResultsFound(message: String) extends RepositoryError
case class PrimaryKeyExists(message: String) extends RepositoryError
case class UniqueKeyExists(message: String) extends RepositoryError
case class FKViolation(message: String) extends RepositoryError
case class NonFatalError(message: String) extends RepositoryError
case class FatalError(message: String, exception: Throwable) extends RepositoryError