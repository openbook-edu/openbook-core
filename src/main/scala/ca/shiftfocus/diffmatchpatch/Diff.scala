package ca.shiftfocus.diffmatchpatch

import name.fraser.neil.plaintext.diff_match_patch
import play.api.libs.json._

case class Diff(
  operation: Operation,
  text: String
) {
  def asJava: diff_match_patch.Diff = {
    new diff_match_patch.Diff(diff_match_patch.Operation.valueOf(operation.value), text)
  }
}

object Diff {

  def apply(javaDiff: diff_match_patch.Diff): Diff = {
    Diff(Operation(javaDiff.operation), javaDiff.text)
  }

}