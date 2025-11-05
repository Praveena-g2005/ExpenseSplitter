package app.utils

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json.Json
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import app.services.AuthService
import play.api.Logging

@Singleton
class AuthAction @Inject() (
  parser: BodyParsers.Default,
  authService: AuthService
)(implicit ec: ExecutionContext)
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with Logging {

  override def parser: BodyParser[AnyContent] = parser
  override def executionContext: ExecutionContext = ec

  override def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] =
    extractToken(request) match {
      case Some(token) =>
        authService.validateAccessToken(token).flatMap {
          case Some((userId, email, role)) =>
            // Construct a lightweight User just for request context
            val user = app.models.User(
              Some(userId),
              "Unknown",
              email,
              "",
              app.models.UserRole.withName(role)
            )
            block(AuthenticatedRequest(user, request))
          case None =>
            Future.successful(
              Unauthorized(Json.obj("error" -> "Invalid or expired token"))
            )
        }
      case None =>
        Future.successful(
          Unauthorized(Json.obj("error" -> "Missing authorization token"))
        )
    }

  private def extractToken[A](request: Request[A]): Option[String] =
    request.headers.get("Authorization").collect {
      case header if header.startsWith("Bearer ") => header.substring(7)
    }
}
