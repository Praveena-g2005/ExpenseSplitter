package app.utils

sealed trait ValidationResult
case object ValidationSuccess extends ValidationResult
case class ValidationFailure(message: String) extends ValidationResult

object Validators {
  private val EMAIL_REGEX =
    """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""".r
  private val MAX_NAME_LENGTH = 255
  private val MAX_EMAIL_LENGTH = 255

  def validateEmail(email: String): ValidationResult = {
    val trimmedemail = email.trim.toLowerCase
    if (trimmedemail.isEmpty) {
      ValidationFailure("Email cannot be empty")
    } else if (trimmedemail.length > MAX_EMAIL_LENGTH) {
      ValidationFailure(
        s"Email should be less than $MAX_EMAIL_LENGTH characters"
      )
    } else if (EMAIL_REGEX.findFirstIn(trimmedemail).isEmpty) {
      ValidationFailure("Invalid Email")
    } else {
      ValidationSuccess
    }
  }

  def validateName(name: String): ValidationResult = {
    val sanitizedname = sanitizeName(name)

    if (sanitizedname.isEmpty) {
      ValidationFailure("Name cannot be empty")
    } else if (sanitizedname.length > MAX_NAME_LENGTH) {
      ValidationFailure(
        s"Name of characters should not exceed $MAX_NAME_LENGTH"
      )
    } else {
      ValidationSuccess
    }
  }

  def validateAmount(
      amount: Double,
      fieldname: String = "amount"
  ): ValidationResult = {
    if (amount <= 0) {
      ValidationFailure("Amount should be positive")
    } else if (amount > 100000) {
      ValidationFailure(s"$fieldname should be less than 1,00,000")
    } else {
      ValidationSuccess
    }
  }
  def sanitizeName(name: String): String = {
    name.trim.replaceAll("\\s+", " ")
  }

  def sanitizeEmail(email: String): String = {
    email.trim.toLowerCase
  }
}
