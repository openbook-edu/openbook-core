package ca.shiftfocus.krispii.core.fail

/**
 * The Fail type is a data type representing the fact that methods asking for
 * a result may not be returned that result for reasons other than program error.
 *
 * Use the Fail type together with the exclusive disjunction (scalaz.\/) to show
 * that a method might not succeed with the given inputs.
 *
 * Throw exceptions when the program itself has encountered an error.
 */
trait Fail { def message: String }

// Operation is not authorized / permissions failure
case class NotAuthorized(message: String) extends Fail

// Operation would violate key constraints (foreign keys, and primary/unique keys)
case class UniqueFieldConflict(message: String) extends Fail
case class ReferenceConflict(message: String) extends Fail

// Referenced entity or entities could not be found
case class NoResults(message: String) extends Fail

// Invalid inputs provided
case class BadInput(message: String) extends Fail

// Optimistic offline lock failure (version out of date)
case class LockFail(message: String) extends Fail

// Operation would violate business logic rules
case class RulesConflict(message: String) extends Fail

// Other failures for which we don't want to throw an exception
case class GenericFail(message: String) extends Fail