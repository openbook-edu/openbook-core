package ca.shiftfocus.krispii.core.fail

trait Fail { def message: String }

case class AuthFail(message: String) extends Fail

case class EntityAlreadyExists(message: String) extends Fail
case class EntityReferenceFieldError(message: String) extends Fail
case class EntityUniqueFieldError(message: String) extends Fail

case class NoResults(message: String) extends Fail
case class BadInput(message: String) extends Fail

case class GenericFail(message: String) extends Fail
case class ExceptionalFail(message: String, exception: Throwable) extends Fail