package app.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.time.Instant
import java.util.Date
import play.api.Configuration
import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

case class JwtClaims(userId: Long, email: String, role: String)

@Singleton
class JwtUtil @Inject() (config: Configuration) {

  private val secret = config.get[String]("jwt.secret")
  private val algorithm = Algorithm.HMAC256(secret)
  private val accessTokenExpiry =
    config.get[Int]("jwt.access-token-expiry") // seconds

  def createAccessToken(userId: Long, email: String, role: String): String = {
    val now = Instant.now()
    val expiry = now.plusSeconds(accessTokenExpiry.toLong)

    JWT
      .create()
      .withSubject(userId.toString)
      .withClaim("email", email)
      .withClaim("role", role)
      .withIssuedAt(Date.from(now))
      .withExpiresAt(Date.from(expiry))
      .sign(algorithm)
  }

  def validateToken(token: String): Try[JwtClaims] =
    Try {
      val verifier = JWT.require(algorithm).build()
      val decoded = verifier.verify(token)
      val userId = decoded.getSubject.toLong
      val email = decoded.getClaim("email").asString()
      val role = decoded.getClaim("role").asString()
      JwtClaims(userId, email, role)
    }
}
