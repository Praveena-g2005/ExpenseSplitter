package app.utils

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json.Json
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import app.services.AuthService
import app.repositories.UserRepository
import play.api.Logging

@Singleton
class AuthAction @Inject() (
    parser: BodyParsers.Default,
    authService: AuthService,
    userRepository: UserRepository
)(implicit ec: ExecutionContext)
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with Logging {

  override def executionContext: ExecutionContext = ec
  override def parser: BodyParser[AnyContent] = parser

  override def invokeBlock[A](
      request: Request[A],
      block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {

    logger.info(
      s"AuthAction: Checking authorization for ${request.method} ${request.uri}"
    )

    extractToken(request) match {
      case Some(token) =>
        logger.info(s"AuthAction: Token found, validating...")
        authService.validateAccessToken(token).flatMap {
          case Some((userId, email, role)) =>
            logger.info(
              s"AuthAction: Token valid for user $userId ($email) with role $role"
            )
            userRepository.findById(userId).flatMap {
              case Some(user) =>
                logger.info(s"AuthAction: User found, proceeding with request")
                block(AuthenticatedRequest(user, request))
              case None =>
                logger.warn(s"AuthAction: User $userId not found in database")
                Future.successful(
                  Unauthorized(Json.obj("error" -> "User not found"))
                )
            }
          case None =>
            logger.warn(s"AuthAction: Token validation failed")
            Future.successful(
              Unauthorized(Json.obj("error" -> "Invalid or expired token"))
            )
        }
      case None =>
        logger.warn(s"AuthAction: No authorization token found in request")
        logger.info(
          s"AuthAction: Headers: ${request.headers.headers.mkString(", ")}"
        )
        Future.successful(
          Unauthorized(Json.obj("error" -> "Missing authorization token"))
        )
    }
  }

  private def extractToken[A](request: Request[A]): Option[String] =
    request.headers.get("Authorization").flatMap { authHeader =>
      logger.info(s"AuthAction: Authorization header: $authHeader")
      if (authHeader.startsWith("Bearer ")) {
        Some(authHeader.substring(7))
      } else {

        logger.warn(
          s"AuthAction: Authorization header doesn't start with 'Bearer '"
        )
        None
      }
    }
}
