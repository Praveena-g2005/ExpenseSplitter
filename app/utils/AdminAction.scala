package app.utils

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json.Json
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import app.services.AuthService
import app.repositories.UserRepository
import app.models.UserRole
import play.api.Logging

@Singleton
class AdminAction @Inject() (
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
      s"AdminAction: Checking admin authorization for ${request.method} ${request.uri}"
    )

    extractToken(request) match {
      case Some(token) =>
        logger.info(s"AdminAction: Token found, validating...")
        authService.validateAccessToken(token).flatMap {
          case Some((userId, email, role)) =>
            logger.info(s"AdminAction: Token valid for user $userId ($email) with role $role")
            
            // Check if user has ADMIN role
            if (role == UserRole.ADMIN.toString) {
              userRepository.findById(userId).flatMap {
                case Some(user) =>
                  logger.info(s"AdminAction: Admin user $userId authorized")
                  block(AuthenticatedRequest(user, request))
                case None =>
                  logger.warn(s"AdminAction: User $userId not found in database")
                  Future.successful(
                    Unauthorized(Json.obj("error" -> "User not found"))
                  )
              }
            } else {
              logger.warn(s"AdminAction: User $userId is not an admin (role: $role)")
              Future.successful(
                Forbidden(Json.obj("error" -> "Admin access required"))
              )
            }
          case None =>
            logger.warn(s"AdminAction: Token validation failed or token revoked")
            Future.successful(
              Unauthorized(Json.obj("error" -> "Invalid, expired, or revoked token"))
            )
        }
      case None =>
        logger.warn(s"AdminAction: No authorization token found in request")
        Future.successful(
          Unauthorized(Json.obj("error" -> "Missing authorization token"))
        )
    }
  }

  private def extractToken[A](request: Request[A]): Option[String] =
    request.headers.get("Authorization").flatMap { authHeader =>
      logger.info(s"AdminAction: Authorization header found")
      if (authHeader.startsWith("Bearer ")) {
        Some(authHeader.substring(7))
      } else {
        logger.warn(
          s"AdminAction: Authorization header doesn't start with 'Bearer '"
        )
        None
      }
    }
}