package ca.shiftfocus.diffmatchpatch

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import name.fraser.neil.plaintext.diff_match_patch
import name.fraser.neil.plaintext.diff_match_patch.Operation

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object DiffMatchPatch {

  /**
   * Internal pointer to an instance of the diff_match_patch engine.
   */
  private val dmf = new diff_match_patch()

  /**
   * Applies a list of patches to a text.
   *
   * @param patches
   * @param text
   * @return
   */
  def patch_apply(patches: List[Patch], text: String): String = {
    val javaList = patch_scalaToJavaLinkedList(patches)
    val result = dmf.patch_apply(javaList, text)

    val patchedText: String = result(0).asInstanceOf[String]
    val patchResults: Array[Boolean] = result(1).asInstanceOf[Array[Boolean]]


    patchedText
  }

  /**
   * Given the original text, and a modified text, create a list of patches required
   * to change one to the other.
   *
   * @param original
   * @param modified
   * @return
   */
  def patch_make(original: String, modified: String): List[Patch] = {
    val result = dmf.patch_make(original, modified)
    patch_javaToScala(result)
  }

  /**
   * Converts a list of patches into a text representation.
   *
   * @param patches
   * @return
   */
  def patch_toText(patches: List[Patch]): String = {
    val javaLL = patch_scalaToJava(patches)
    dmf.patch_toText(javaLL)
  }

  /**
   * Converts a text representation of patches into a List of patches.
   *
   * @param text
   * @return
   */
  def patch_fromText(text: String): List[Patch] = {
    val javaPatches = dmf.patch_fromText(text)
    patch_javaToScala(javaPatches)
  }


  /*
   * Here are some potentially helpful converter methods, for moving between our Scala representations
   * and the library's java representations.
   */

  def patch_javaToScala(patches: java.util.List[diff_match_patch.Patch]): List[Patch] = {
    patches.asScala.toList.map { patch =>
      Patch(patch)
    }
  }

  def patch_javaToScala(patches: java.util.LinkedList[diff_match_patch.Patch]): List[Patch] = {
    patches.asScala.toList.map { patch =>
      Patch(patch)
    }
  }

  def patch_scalaToJava(patches: List[Patch]): java.util.List[diff_match_patch.Patch] = {
    new java.util.LinkedList[diff_match_patch.Patch](patches.map(_.asJava).asJavaCollection)
  }

  def patch_scalaToJavaLinkedList(patches: List[Patch]): java.util.LinkedList[diff_match_patch.Patch] = {
    new java.util.LinkedList[diff_match_patch.Patch](patches.map(_.asJava).asJavaCollection)
  }

}

