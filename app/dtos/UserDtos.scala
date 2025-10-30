package app.dtos

import play.api.libs.json._
import app.models.User

case class CreateUserRequest(
  name: String,
  email: String,
  password: String
)

case class UserResponse(
  id: Long,
  name: String,
  email: String,
  role: String
)

object CreateUserRequest {
  implicit val format: OFormat[CreateUserRequest] =
    Json.format[CreateUserRequest]
}

object UserResponse {
  implicit val format: OFormat[UserResponse] = Json.format[UserResponse]
  def fromUser(user: User): UserResponse =
    UserResponse(
      id = user.id.getOrElse(
        throw new IllegalStateException("User must have an ID")
      ),
      name = user.name,
      email = user.email,
      role = user.role.toString
    )
}

case class ErrorResponse(
  error: String,
  details: Option[Map[String, String]] = None
)
object ErrorResponse {
  implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}
