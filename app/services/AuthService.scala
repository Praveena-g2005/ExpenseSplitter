package app.services

import app.models.{RefreshToken, RevokedToken, User}
import app.repositories.{RefreshTokenRepository, RevokedTokenRepository, UserRepository}
import app.utils.{JwtUtil, PasswordHasher}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

case class AuthTokens(accessToken: String, refreshToken: String, expiresIn: Int)
case class LoginResult(tokens: AuthTokens, user: User)

@Singleton
class AuthService @Inject() (
  userRepository: UserRepository,
  refreshTokenRepository: RefreshTokenRepository,
  revokedTokenRepository: RevokedTokenRepository,
  jwtUtil: JwtUtil
)(implicit ec: ExecutionContext)
    extends Logging {

  private val refreshTokenExpiry = 7 // days
  private val accessTokenExpiry = 900 // 15 minutes in seconds

  def login(
    email: String,
    password: String
  ): Future[Either[String, LoginResult]] = {
    logger.info(s"Login attempt for email: $email")

    userRepository.findByEmailWithPassword(email).flatMap {
      case Some(user) =>
        if (PasswordHasher.verify(password, user.passwordHash)) {
          // Revoke all existing tokens for this user
          refreshTokenRepository.revokeUserTokens(user.id.get).flatMap { _ =>
            val accessToken = jwtUtil
              .createAccessToken(user.id.get, user.email, user.role.toString)
            val refreshTokenString = UUID.randomUUID().toString

            val expiresAt = Timestamp
              .valueOf(LocalDateTime.now().plusDays(refreshTokenExpiry))

            val refreshToken = RefreshToken(
              id = None,
              userId = user.id.get,
              token = refreshTokenString,
              expiresAt = expiresAt
            )

            refreshTokenRepository.create(refreshToken).map { _ =>
              logger.info(s"Login successful for user: ${user.id}")
              Right(
                LoginResult(
                  tokens = AuthTokens(
                    accessToken,
                    refreshTokenString,
                    accessTokenExpiry
                  ),
                  user = user
                )
              )
            }
          }
        } else {
          logger.warn(s"Invalid password for email: $email")
          Future.successful(Left("Invalid email or password"))
        }

      case None =>
        logger.warn(s"User not found: $email")
        Future.successful(Left("Invalid email or password"))
    }
  }

  def refreshAccessToken(
    refreshToken: String
  ): Future[Either[String, String]] = {
    logger.info("Refreshing access token")

    refreshTokenRepository.isValid(refreshToken).flatMap {
      case true =>
        refreshTokenRepository.findByToken(refreshToken).flatMap {
          case Some(token) =>
            userRepository.findById(token.userId).map {
              case Some(user) =>
                val newAccessToken = jwtUtil.createAccessToken(
                  user.id.get,
                  user.email,
                  user.role.toString
                )
                Right(newAccessToken)
              case None =>
                Left("User not found")
            }
          case None =>
            Future.successful(Left("Invalid refresh token"))
        }
      case false =>
        Future.successful(Left("Refresh token expired or revoked"))
    }
  }

  def logout(refreshToken: String): Future[Boolean] = {
    logger.info("Revoking refresh token")
    refreshTokenRepository.revokeToken(refreshToken).map(_ > 0)
  }

  def revokeAccessToken(accessToken: String, userId: Long): Future[Boolean] = {
    logger.info(s"Revoking access token for user: $userId")

    jwtUtil.validateToken(accessToken) match {
      case scala.util.Success(claims) =>
        val now = Timestamp.valueOf(LocalDateTime.now())
        // Access tokens expire in 15 minutes
        val expiresAt =
          Timestamp.valueOf(LocalDateTime.now().plusSeconds(accessTokenExpiry))

        val revokedToken = RevokedToken(
          id = None,
          token = accessToken,
          userId = userId,
          tokenType = "ACCESS",
          revokedAt = now,
          expiresAt = expiresAt
        )

        revokedTokenRepository
          .create(revokedToken)
          .map { _ =>
            logger.info(s"Access token revoked for user: $userId")
            true
          }
          .recover {
            case ex: Exception =>
              logger.error(s"Failed to revoke access token: ${ex.getMessage}", ex)
              false
          }

      case scala.util.Failure(ex) =>
        logger.warn(
          s"Invalid access token provided for revocation: ${ex.getMessage}"
        )
        Future.successful(false)
    }
  }

  def validateAccessToken(
    token: String
  ): Future[Option[(Long, String, String)]] =
    // First check if token is revoked
    revokedTokenRepository.isTokenRevoked(token).flatMap {
      case true =>
        logger.info("Token is revoked")
        Future.successful(None)
      case false =>
        // Then validate JWT
        jwtUtil.validateToken(token) match {
          case scala.util.Success(claims) =>
            Future.successful(Some((claims.userId, claims.email, claims.role)))
          case scala.util.Failure(ex) =>
            logger.warn(s"Token validation failed: ${ex.getMessage}")
            Future.successful(None)
        }
    }
}
