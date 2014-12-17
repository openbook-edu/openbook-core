package ca.shiftfocus.diffmatchpatch

import name.fraser.neil.plaintext.diff_match_patch

trait Operation {
  val value: String
}

object Operation {
  def apply(op: diff_match_patch.Operation): Operation = {
    op match {
      case insert if insert == diff_match_patch.Operation.valueOf("INSERT") => Insert()
      case delete if delete == diff_match_patch.Operation.valueOf("DELETE") => Delete()
      case equal if equal == diff_match_patch.Operation.valueOf("EQUAL") => Equal()
      case _ => throw new Exception("Unknown operation type.")
    }
  }
}

case class Insert(value: String = "INSERT") extends Operation
case class Delete(value: String = "DELETE") extends Operation
case class Equal(value: String = "EQUAL") extends Operation
