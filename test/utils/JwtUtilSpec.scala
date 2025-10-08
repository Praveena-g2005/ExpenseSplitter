package utils

import app.utils.JwtUtil
import org.scalatestplus.play._
import play.api.Configuration

class JwtUtilSpec extends PlaySpec {

  val config = Configuration(
    "jwt.secret" -> "test-secret-key",
    "jwt.access-token-expiry" -> 900
  )

  val jwtUtil = new JwtUtil(config)

  "JwtUtil" should {

    "create valid access tokens" in {
      val token = jwtUtil.createAccessToken(1L, "test@example.com")

      token must not be empty
      token must startWith("eyJ")
    }

    "validate correct tokens" in {
      val token = jwtUtil.createAccessToken(1L, "test@example.com")

      val result = jwtUtil.validateToken(token)

      result.isSuccess mustBe true
      result.get.userId mustBe 1L
      result.get.email mustBe "test@example.com"
    }

    "reject invalid tokens" in {
      val invalidToken = "invalid.token.here"

      val result = jwtUtil.validateToken(invalidToken)

      result.isFailure mustBe true
    }

    "reject tampered tokens" in {
      val token = jwtUtil.createAccessToken(1L, "test@example.com")
      val tamperedToken = token.dropRight(5) + "xxxxx"

      val result = jwtUtil.validateToken(tamperedToken)

      result.isFailure mustBe true
    }
  }
}
