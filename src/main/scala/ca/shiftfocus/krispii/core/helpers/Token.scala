package ca.shiftfocus.krispii.core.helpers

import java.nio.ByteBuffer
import java.security.{ MessageDigest, SecureRandom }

/**
 * Created by ryanez on 16/02/16.
 * This a copy of the Nonce object used in the LCEEQ project to generate the token of authentication when
 * a new user is being created.
 */
object Token {
  val secureRandom = SecureRandom.getInstance("SHA1PRNG")

  /**
   * Generate a reasonably secure random nonce for use as a user activation token.
   */
  def getNext = {
    val number = secureRandom.nextLong()
    val bytes = ByteBuffer.allocate(8).putLong(number).array()
    val hashedBytes = MessageDigest.getInstance("SHA-256").digest(bytes)
    val nonce = HexBytesUtil.bytes2hex(hashedBytes)
    nonce
  }
}

object HexBytesUtil {

  def hex2bytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String = {
    sep match {
      case None => bytes.map("%02x".format(_)).mkString
      case _ => bytes.map("%02x".format(_)).mkString(sep.get)
    }
    // bytes.foreach(println)
  }

  def example() {
    val data = "48 65 6C 6C 6F 20 57 6F 72 6C 64 21 21"
    val bytes = hex2bytes(data)
    println(bytes2hex(bytes, Option(" ")))

    val data2 = "48-65-6C-6C-6F-20-57-6F-72-6C-64-21-21"
    val bytes2 = hex2bytes(data2)
    println(bytes2hex(bytes2, Option("-")))

    val data3 = "48656C6C6F20576F726C642121"
    val bytes3 = hex2bytes(data3)
    println(bytes2hex(bytes3))
  }

}
