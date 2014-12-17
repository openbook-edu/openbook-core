package ca.shiftfocus.diffmatchpatch

import name.fraser.neil.plaintext.diff_match_patch
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._


case class Patch(
  diffs: List[Diff],
  start1: Int,
  start2: Int,
  length1: Int,
  length2: Int
) {
  def asJava: diff_match_patch.Patch = {
    val javaPatch = new diff_match_patch.Patch
    val javaDiffs = new java.util.LinkedList[diff_match_patch.Diff](this.diffs.map(_.asJava).asJavaCollection)
    javaPatch.diffs = javaDiffs
    javaPatch.start1 = this.start1
    javaPatch.start2 = this.start2
    javaPatch.length1 = this.length1
    javaPatch.length2 = this.length2
    javaPatch
  }
}

object Patch {

  /**
   * Build a Patch from a Java patch.
   * @param javaPatch
   * @return
   */
  def apply(javaPatch: diff_match_patch.Patch): Patch = {
    val diffs = javaPatch.diffs.asScala.toList.map({diff => Diff(diff)})
    Patch(diffs, javaPatch.start1, javaPatch.start2, javaPatch.length1, javaPatch.length2)
  }

}