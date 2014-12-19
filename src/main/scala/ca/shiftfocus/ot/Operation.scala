spackage ca.shiftfocus.ot

import play.api.libs.json._

/*

Operation chain example:

  Consider the two operations chained together:
    retain 8, insert "cats"
    retain 30, insert "dogs"

  They can be written as one single operation:
    C -> retain 8, insert "cats", retain 18, insert dogs

  Let's say we have another operation that was just committed onto the server.
    S -> retain 10, insert "frogs", retain 16, insert "bees"

  How should these be transformed? Well, now we can treat them each as one composite operation.
    C' -> retain 8, insert "cats", retain 27, insert "dogs"
    S' -> retain 14, insert "frogs", retain 16, insert "bees"

  Two simple rules:
  - Inserts by one party will shift the later retain statements of the other party rightward, while Deletes will shift them leftward.
  - The server always wins. The two operations don't actually happen simultaneously. We assume the server acted first,
    and we modify the client to be acting on the server's new reality. The transformations simply ensure that both parties
    will converge to the server's reality.

 */

sealed trait OperationComponent

case class Retain(numChars: Int) extends OperationComponent

case class Insert(chars: String) extends OperationComponent

case class Delete(chars: String) extends OperationComponent

/**
 * An operation is defined as a chain of operation components. Sequential operations can themselves be composed into new
 * operations.
 *
 * @param components
 */
case class Operation(components: IndexedSeq[OperationComponent]) {

  def :+(component: OperationComponent): Operation = {
    Operation(components :+ component)
  }

  def +:(component: OperationComponent): Operation = {
    Operation(component +: components)
  }

  /**
   * Concatenation is the tricky operation. We assume that the second operation is performed on the first operation's
   * changed reality, but we want to apply them together in one sequence without "starting over" from the beginning of
   * the text. We'll need to transform the second operation against the first and "zip" them together.
   *
   * Example:
   *
   * Here are two operations, A and B. The user first performed A, and then since the timeout hadn't expired, they
   * also performed operation B. The user wants to send both operations to the server as a compound operation.
   *
   *   Original text: this is a text about fuzzy little animals
   *   First change:  this is a cat text about fuzzy non-dog little animals
   *   Second change: this is a catty text about fuzzy non-dog animals
   *
   *   A -> retain 9, insert "cat ", retain 17, insert "non-dog ", retain 14
   *   B -> retain 13, insert "ty", retain 26, delete "little ", retain 7
   *
   * What should the new compound action look like?
   *   AB -> retain 9, insert "catty ", retain 17, insert "non-dog ", delete "little ", retain 7
   *
   * What do you notice about the compound action? When the second's insert happened inside the first, it should be split
   * into three inserts which we can then condense into one. Operation A inserted "cat ", and Operation B inserted "ty"
   * between the 't' and ' '.
   *
   * NB: String lengths:
   *     A: 41 -> 53
   *     B: 53 -> 48
   *
   * We can treat each insert and delete as happening at an index in the string.
   *    A: insert(9, "cat "), insert(30, "non-dog ")
   *    B: insert(13, "ty"), delete(38, "little ")
   *
   * The key here is to detect when two operations would collide, ie: operation B inserts text into the middle of
   * something inserted by operation A. Thus, the two insertions should be merged. Likewise if operation B deletes
   * something inserted by operation A, the insert should be trimmed.
   *
   *
   * @param that
   * @return
   */
  def ++(that: Operation): Operation = {

  }

}

