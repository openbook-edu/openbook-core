package ca.shiftfocus.krispii.core.error

// This trait will represent the "union" of both error types
sealed trait ErrorUnion {
  sealed trait Fail
}

// Repository errors
sealed trait DatabaseErrorT extends ErrorUnion {
  case class DatabaseError(message: String) extends Fail
}
sealed trait NoResultsT extends ErrorUnion {
  case class NoResults(message: String) extends Fail
}
sealed trait UniqueKeyConflictT extends ErrorUnion {
  case class UniqueKeyConflict(message: String) extends Fail
}
sealed trait ForeignKeyConflictT extends ErrorUnion {
  case class ForeignKeyConflict(message: String) extends Fail
}
sealed trait OfflineLockFailT extends ErrorUnion {
  case class OfflineLockFail(message: String) extends Fail
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

// Now we can construct two concrete ADT's out of of the above traits
object RepositoryError
  extends DatabaseErrorT
  with NoResultsT
  with UniqueKeyConflictT
  with ForeignKeyConflictT
  with OfflineLockFailT

object ServiceError
  extends BadInputT
  with BadPermissionT
  with BusinessLogicFailT

// And an exception for when you don't want to expect particular fails
case class UnexpectedFailException(fail: ErrorUnion#Fail) extends Exception
