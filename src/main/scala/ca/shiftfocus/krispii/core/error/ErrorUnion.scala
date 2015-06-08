package ca.shiftfocus.krispii.core.error

// This trait will represent the "union" of both error types
sealed trait ErrorUnion {
  sealed trait Fail
}

// Repository errors
sealed trait DatabaseErrorT extends ErrorUnion {
  case class DatabaseError(message: String, exception: Option[Throwable] = None) extends Fail
}
sealed trait NoResultsT extends ErrorUnion {
  case class NoResults(message: String) extends Fail
}
sealed trait PrimaryKeyConflictT extends ErrorUnion {
  object PrimaryKeyConflict extends Fail
}
sealed trait UniqueKeyConflictT extends ErrorUnion {
  case class UniqueKeyConflict(column: String, constraint: String) extends Fail
}
sealed trait ForeignKeyConflictT extends ErrorUnion {
  case class ForeignKeyConflict(column: String, constraint: String) extends Fail
}
sealed trait OfflineLockFailT extends ErrorUnion {
  object OfflineLockFail extends Fail
}

sealed trait BadParamT extends ErrorUnion {
  case class BadParam(message: String) extends Fail
}

// Service errors
sealed trait BadInputT extends ErrorUnion {
  case class BadInput(message: String) extends Fail
}
sealed trait BadPermissionT extends ErrorUnion {
  case class BadPermissions(message: String) extends Fail
}
sealed trait BusinessLogicFailT extends ErrorUnion {
  case class BusinessLogicFail(message: String) extends Fail
}
sealed trait NotScheduledT extends ErrorUnion {
  object NotScheduled extends Fail
}

// Now we can construct two concrete ADT's out of of the above traits
object RepositoryError
  extends DatabaseErrorT
  with NoResultsT
  with PrimaryKeyConflictT
  with UniqueKeyConflictT
  with ForeignKeyConflictT
  with OfflineLockFailT
  with BadParamT

object ServiceError
  extends BadInputT
  with BadPermissionT
  with BusinessLogicFailT
  with NotScheduledT
  with OfflineLockFailT

// And an exception for when you don't want to expect particular fails
case class UnexpectedFailException(fail: ErrorUnion#Fail) extends Exception
