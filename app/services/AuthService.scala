package app.services

import app.models.User
import app.repositories.UserRepository
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
  jwtUtil: JwtUtil
)(implicit ec: ExecutionContext)
    extends Logging {
  private val accessTokenExpirySeconds = 900 // 15 minutes
  private val refreshTokenExpiryDays = 7 // 7 days

  def login(
    email: String,
    password: String
  ): Future[Either[String, LoginResult]] = {
    logger.info(s"Login attempt for email: $email")

    userRepository.findByEmailWithPassword(email).map {
      case Some(user) =>
        if (PasswordHasher.verify(password, user.passwordHash)) {
          val accessToken = jwtUtil.createAccessToken(
            userId = user.id.get,
            email = user.email,
            role = user.role.toString
          )

          val refreshToken = jwtUtil.createRefreshToken(
            userId = user.id.get,
            email = user.email,
            role = user.role.toString
          )

          Right(
            LoginResult(
              tokens = AuthTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = accessTokenExpirySeconds
              ),
              user = user
            )
          )
        } else {
          logger.warn(s"Invalid password for email: $email")
          Left("Invalid email or password")
        }

      case None =>
        logger.warn(s"User not found: $email")
        Left("Invalid email or password")
    }
  }

  def refreshAccessToken(refreshToken: String): Future[Either[String, String]] = {
    logger.info("Refreshing access token (stateless)")

    jwtUtil.validateRefreshToken(refreshToken) match {
      case scala.util.Success(claims) =>
        // claims contain userId, email, role
        val newAccessToken = jwtUtil.createAccessToken(
          userId = claims.userId,
          email = claims.email,
          role = claims.role
        )
        Future.successful(Right(newAccessToken))

      case scala.util.Failure(ex) =>
        logger.warn(s"Invalid/expired refresh token: ${ex.getMessage}")
        Future.successful(Left("Invalid or expired refresh token"))
    }
  }

  // Keeping method for controller compatibility;
  def logout(refreshToken: String): Future[Boolean] = {
    logger.info("Logout called (stateless) - no DB revoke performed")
    Future.successful(true)
  }

  def revokeAccessToken(accessToken: String, userId: Long): Future[Boolean] = {
    logger.info("revokeAccessToken called (stateless) - no-op")
    Future.successful(false)
  }

  def validateAccessToken(
    token: String
  ): Future[Option[(Long, String, String)]] =
    jwtUtil.validateAccessToken(token) match {
      case scala.util.Success(claims) =>
        Future.successful(Some((claims.userId, claims.email, claims.role)))
      case scala.util.Failure(ex) =>
        logger.warn(s"Token validation failed: ${ex.getMessage}")
        Future.successful(None)
    }
}
