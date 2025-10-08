package app.controllers

import app.services.{UserService, AuthService}
import play.api.libs.json._
import play.api.mvc._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging

case class RegisterRequest(name: String, email: String, password: String)
object RegisterRequest {
  implicit val format: OFormat[RegisterRequest] = Json.format[RegisterRequest]
}

case class LoginRequest(email: String, password: String)
object LoginRequest {
  implicit val format: OFormat[LoginRequest] = Json.format[LoginRequest]
}

case class RefreshRequest(refreshToken: String)
object RefreshRequest {
  implicit val format: OFormat[RefreshRequest] = Json.format[RefreshRequest]
}

case class LoginResponse(
    accessToken: String,
    refreshToken: String,
    expiresIn: Int,
    user: UserInfo
)
object LoginResponse {
  implicit val format: OFormat[LoginResponse] = Json.format[LoginResponse]
}

case class UserInfo(id: Long, name: String, email: String)
object UserInfo {
  implicit val format: OFormat[UserInfo] = Json.format[UserInfo]
}

case class RefreshResponse(accessToken: String, expiresIn: Int)
object RefreshResponse {
  implicit val format: OFormat[RefreshResponse] = Json.format[RefreshResponse]
}

case class ErrorResponse(error: String)
object ErrorResponse {
  implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

@Singleton
class AuthController @Inject() (
    cc: ControllerComponents,
    userService: UserService,
    authService: AuthService
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with Logging {

  def register(): Action[JsValue] = Action.async(parse.json) { request =>
    logger.info("Register request received")

    request.body.validate[RegisterRequest] match {
      case JsSuccess(registerRequest, _) =>
        userService
          .createUser(
            registerRequest.name,
            registerRequest.email,
            registerRequest.password
          )
          .map {
            case Right(user) =>
              logger.info(s"User registered: ${user.id}")
              Created(
                Json.obj(
                  "message" -> "User registered successfully",
                  "user" -> Json.obj(
                    "id" -> user.id,
                    "name" -> user.name,
                    "email" -> user.email
                  )
                )
              )
            case Left(error) =>
              logger.warn(s"Registration failed: $error")
              BadRequest(Json.toJson(ErrorResponse(error)))
          }
      case JsError(_) =>
        Future.successful(
          BadRequest(Json.toJson(ErrorResponse("Invalid JSON")))
        )
    }
  }

  def login(): Action[JsValue] = Action.async(parse.json) { request =>
    logger.info("Login request received")

    request.body.validate[LoginRequest] match {
      case JsSuccess(loginRequest, _) =>
        authService.login(loginRequest.email, loginRequest.password).map {
          case Right(loginResult) =>
            logger.info(s"Login successful for user: ${loginResult.user.id}")
            Ok(
              Json.toJson(
                LoginResponse(
                  accessToken = loginResult.tokens.accessToken,
                  refreshToken = loginResult.tokens.refreshToken,
                  expiresIn = loginResult.tokens.expiresIn,
                  user = UserInfo(
                    id = loginResult.user.id.get,
                    name = loginResult.user.name,
                    email = loginResult.user.email
                  )
                )
              )
            )
          case Left(error) =>
            logger.warn(s"Login failed: $error")
            Unauthorized(Json.toJson(ErrorResponse(error)))
        }
      case JsError(_) =>
        Future.successful(
          BadRequest(Json.toJson(ErrorResponse("Invalid JSON")))
        )
    }
  }

  def refresh(): Action[JsValue] = Action.async(parse.json) { request =>
    logger.info("Refresh token request received")

    request.body.validate[RefreshRequest] match {
      case JsSuccess(refreshRequest, _) =>
        authService.refreshAccessToken(refreshRequest.refreshToken).map {
          case Right(accessToken) =>
            Ok(Json.toJson(RefreshResponse(accessToken, 900)))
          case Left(error) =>
            Unauthorized(Json.toJson(ErrorResponse(error)))
        }
      case JsError(_) =>
        Future.successful(
          BadRequest(Json.toJson(ErrorResponse("Invalid JSON")))
        )
    }
  }

  def logout(): Action[JsValue] = Action.async(parse.json) { request =>
    logger.info("Logout request received")

    request.body.validate[RefreshRequest] match {
      case JsSuccess(logoutRequest, _) =>
        authService.logout(logoutRequest.refreshToken).map { success =>
          if (success) {
            Ok(Json.obj("message" -> "Logged out successfully"))
          } else {
            BadRequest(Json.toJson(ErrorResponse("Invalid refresh token")))
          }
        }
      case JsError(_) =>
        Future.successful(
          BadRequest(Json.toJson(ErrorResponse("Invalid JSON")))
        )
    }
  }
}
