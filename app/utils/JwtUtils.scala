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
  private val accessSecret = config.get[String]("jwt.secret.access")
  private val refreshSecret = config.get[String]("jwt.secret.refresh")
  private val accessExpirySeconds = config.get[Int]("jwt.access-token-expiry") // 900 (15 min)
  private val refreshExpirySeconds = config.get[Int]("jwt.refresh-token-expiry") // 7*24*3600 = 604800

  private val accessAlg = Algorithm.HMAC256(accessSecret)
  private val refreshAlg = Algorithm.HMAC256(refreshSecret)

  /** Generate short-lived access token */
  def createAccessToken(userId: Long, email: String, role: String): String = {
    val now = Instant.now()
    val expiry = now.plusSeconds(accessExpirySeconds.toLong)
    JWT
      .create()
      .withSubject(userId.toString)
      .withClaim("email", email)
      .withClaim("role", role)
      .withIssuedAt(Date.from(now))
      .withExpiresAt(Date.from(expiry))
      .sign(accessAlg)
  }

  /** Generate long-lived refresh token */
  def createRefreshToken(userId: Long, email: String, role: String): String = {
    val now = Instant.now()
    val expiry = now.plusSeconds(refreshExpirySeconds.toLong)
    JWT
      .create()
      .withSubject(userId.toString)
      .withClaim("email", email)
      .withClaim("role", role)
      .withIssuedAt(Date.from(now))
      .withExpiresAt(Date.from(expiry))
      .sign(refreshAlg)
  }

  /** Validate access token and extract claims */
  def validateAccessToken(token: String): Try[JwtClaims] = validate(token, accessAlg)

  /** Validate refresh token and extract claims */
  def validateRefreshToken(token: String): Try[JwtClaims] = validate(token, refreshAlg)

  /** Shared validator */
  private def validate(token: String, algorithm: Algorithm): Try[JwtClaims] = Try {
    val verifier = JWT.require(algorithm).build()
    val decoded = verifier.verify(token)
    JwtClaims(
      userId = decoded.getSubject.toLong,
      email = decoded.getClaim("email").asString(),
      role = decoded.getClaim("role").asString()
    )
  }
}
