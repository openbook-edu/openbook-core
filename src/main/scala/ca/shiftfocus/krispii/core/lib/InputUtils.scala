package ca.shiftfocus.krispii.core.lib

object InputUtils {
  /**
   * Validate e-mail address.
   *
   * @param email
   * @return
   */
  def isValidEmail(email: String): Boolean = {
    val parts = email.split("@")

    !(parts.length != 2 ||
      !parts(0).charAt(0).isLetterOrDigit ||
      !parts(1).charAt(parts(1).length - 1).isLetter ||
      parts(1).indexOf("..") != -1 ||
      !"""^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r.unapplySeq(email.trim).isDefined)
  }

  /**
   * Validate username.
   *
   * @param username
   * @return
   */
  def isValidUsername(username: String): Boolean = {
    username.length >= 3
  }

  /**
   * Validate password.
   *
   * @param password
   * @return
   */
  def isValidPassword(password: String): Boolean = {
    password.length >= 8
  }
}
