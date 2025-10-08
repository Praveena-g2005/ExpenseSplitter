package utils

import app.utils.PasswordHasher
import org.scalatestplus.play._

class PasswordHasherSpec extends PlaySpec {

  "PasswordHasher" should {

    "hash passwords correctly" in {
      val password = "TestPassword123"
      val hash = PasswordHasher.hash(password)

      hash must not be password
      hash.length must be > 20
    }

    "verify correct password" in {
      val password = "TestPassword123"
      val hash = PasswordHasher.hash(password)

      PasswordHasher.verify(password, hash) mustBe true
    }

    "reject incorrect password" in {
      val password = "TestPassword123"
      val hash = PasswordHasher.hash(password)

      PasswordHasher.verify("WrongPassword", hash) mustBe false
    }

    "generate different hashes for same password" in {
      val password = "TestPassword123"
      val hash1 = PasswordHasher.hash(password)
      val hash2 = PasswordHasher.hash(password)

      hash1 must not equal hash2
    }
  }
}
