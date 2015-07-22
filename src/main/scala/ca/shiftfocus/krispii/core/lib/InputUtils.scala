package ca.shiftfocus.krispii.core.lib

import ca.shiftfocus.krispii.core.error.{ErrorUnion, ServiceError}

import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

/**
 * Created by aostapenco on 7/22/15.
 */
object InputUtils {
  /**
   * Validate e-mail address.
   *
   * @param email
   * @return
   */
  def isValidEmail(email: String): Future[\/[ErrorUnion#Fail, String]] = Future.successful {
    val parts = email.split("@")
    if (parts.length != 2 ||
      !parts(0).charAt(0).isLetter ||
      !parts(1).charAt(parts(1).length - 1).isLetter ||
      parts(1).indexOf("..") != -1 ||
      !"""([\w\._-]+)@([\w\._-]+)""".r.unapplySeq(email.trim).isDefined) {
      \/.left(ServiceError.BadInput(s"'$email' is not a valid format"))
    }
    else {
      \/.right(email.trim)
    }
  }

  /**
   * Validate username.
   *
   * @param username
   * @return
   */
  def isValidUsername(username: String): Future[\/[ErrorUnion#Fail, String]] = Future.successful {
    if (username.length >= 3) \/-(username)
    else -\/(ServiceError.BadInput("Your username must be at least 3 characters."))
  }

  /**
   * Validate password.
   *
   * @param password
   * @return
   */
  def isValidPassword(password: String): Future[\/[ErrorUnion#Fail, String]] = Future.successful {
    if (password.length >= 8) \/-(password)
    else -\/(ServiceError.BadInput("The password provided must be at least 8 characters."))
  }
}
