package app.services

import app.models.{User, RefreshToken}
import app.repositories.{UserRepository, RefreshTokenRepository}
import app.utils.{PasswordHasher, JwtUtil}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import java.time.LocalDateTime
import java.util.UUID

case class AuthTokens(accessToken: String, refreshToken: String, expiresIn: Int)
case class LoginResult(tokens: AuthTokens, user: User)

@Singleton
class AuthService @Inject() (
    userRepository: UserRepository,
    refreshTokenRepository: RefreshTokenRepository,
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
          // Revoke old tokens
          refreshTokenRepository.revokeUserTokens(user.id.get).flatMap { _ =>
            // Create new tokens
            val accessToken = jwtUtil.createAccessToken(user.id.get, user.email)
            val refreshTokenString = UUID.randomUUID().toString
            val refreshToken = RefreshToken(
              id = None,
              userId = user.id.get,
              token = refreshTokenString,
              expiresAt = LocalDateTime.now().plusDays(refreshTokenExpiry)
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
                val newAccessToken =
                  jwtUtil.createAccessToken(user.id.get, user.email)
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
    logger.info("Logging out user")
    refreshTokenRepository.revokeToken(refreshToken).map(_ > 0)
  }

  def validateAccessToken(token: String): Option[(Long, String)] = {
    jwtUtil
      .validateToken(token)
      .toOption
      .map(claims => (claims.userId, claims.email))
  }
}
