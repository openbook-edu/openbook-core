package ca.shiftfocus.krispii.core.fail

trait Fail { def message: String }

// Operation is not authorized / permissions failure
case class NotAuthorized(message: String) extends Fail

// Operation would violate business logic rules
case class RulesConflict(message: String) extends Fail

// Operation would violate key constraints (foreign keys, and primary/unique keys)
case class UniqueFieldConflict(message: String) extends Fail
case class ReferenceConflict(message: String) extends Fail

// Referenced entity or entities could not be found
case class NoResults(message: String) extends Fail

// Invalid inputs provided
case class BadInput(message: String) extends Fail

// Optimistic offline lock failure (version out of date)
case class LockFail(message: String) extends Fail

// Other client failure
case class GenericFail(message: String) extends Fail