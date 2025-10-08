package utils

import app.utils.{ValidationFailure, ValidationSuccess, Validators}
import org.scalatestplus.play._

class ValidatorsSpec extends PlaySpec {

  "Validators.validateEmail" should {

    "accept valid emails" in {
      Validators.validateEmail("test@example.com") mustBe ValidationSuccess
      Validators.validateEmail("user.name@domain.co.uk") mustBe ValidationSuccess
      Validators.validateEmail("user+tag@example.com") mustBe ValidationSuccess
    }

    "reject empty email" in {
      Validators.validateEmail("") match {
        case ValidationFailure(msg) => msg must include("empty")
        case _                      => fail("Should reject empty email")
      }
    }

    "reject invalid email format" in {
      Validators.validateEmail("notanemail") match {
        case ValidationFailure(msg) => msg must include("Invalid")
        case _                      => fail("Should reject invalid email")
      }

      Validators.validateEmail("missing@domain") match {
        case ValidationFailure(_) => succeed
        case _                    => fail("Should reject email without TLD")
      }
    }

    "reject emails that are too long" in {
      val longEmail = "a" * 300 + "@example.com"
      Validators.validateEmail(longEmail) match {
        case ValidationFailure(msg) => msg must include("less than")
        case _                      => fail("Should reject long email")
      }
    }
  }

  "Validators.validateName" should {

    "accept valid names" in {
      Validators.validateName("John Doe") mustBe ValidationSuccess
      Validators.validateName("Mary Jane") mustBe ValidationSuccess
    }

    "reject empty name" in {
      Validators.validateName("") match {
        case ValidationFailure(msg) => msg must include("empty")
        case _                      => fail("Should reject empty name")
      }
    }

    "sanitize multiple spaces" in {
      val sanitized = Validators.sanitizeName("John    Doe")
      sanitized mustBe "John Doe"
    }

    "reject names that are too long" in {
      val longName = "a" * 300
      Validators.validateName(longName) match {
        case ValidationFailure(msg) => msg must include("should not exceed")
        case _                      => fail("Should reject long name")
      }
    }
  }

  "Validators.validateAmount" should {

    "accept valid amounts" in {
      Validators.validateAmount(100.0) mustBe ValidationSuccess
      Validators.validateAmount(999.99) mustBe ValidationSuccess
    }

    "reject zero or negative amounts" in {
      Validators.validateAmount(0) match {
        case ValidationFailure(msg) => msg must include("positive")
        case _                      => fail("Should reject zero")
      }

      Validators.validateAmount(-50) match {
        case ValidationFailure(msg) => msg must include("positive")
        case _                      => fail("Should reject negative")
      }
    }

    "reject amounts that are too large" in {
      Validators.validateAmount(200000) match {
        case ValidationFailure(msg) => msg must include("less than")
        case _                      => fail("Should reject large amount")
      }
    }
  }
}
