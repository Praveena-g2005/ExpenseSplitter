package app.models

import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

object UserRole extends Enumeration {
  type UserRole = Value
  val ADMIN, USER = Value

  implicit val userRoleFormat: Format[UserRole] = new Format[UserRole] {
    def reads(json: JsValue): JsResult[UserRole] = json match {
      case JsString(s) =>
        try
          JsSuccess(UserRole.withName(s.toUpperCase))
        catch {
          case _: NoSuchElementException => JsError(s"Invalid role: $s")
        }
      case _ => JsError("String expected for role")
    }

    def writes(role: UserRole): JsValue = JsString(role.toString)
  }
}

case class User(
  id: Option[Long] = None,
  name: String,
  email: String,
  passwordHash: String,
  role: UserRole.UserRole = UserRole.USER // default is user role
)
object User {
  import UserRole.userRoleFormat
  implicit val Format: OFormat[User] = Json.format[User]
}

class UserTable(tag: Tag) extends Table[User](tag, "users") {
  import UserRole._

  // Custom column type for Role enum
  implicit val roleMapper = MappedColumnType.base[UserRole, String](
    role => role.toString,
    str => UserRole.withName(str)
  )

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name: Rep[String] = column[String]("name")
  def email: Rep[String] = column[String]("email")
  def passwordHash: Rep[String] = column[String]("password_hash")
  def role: Rep[UserRole] = column[UserRole]("role", O.Default(UserRole.USER))

  def * : ProvenShape[User] =
    (
      id.?,
      name,
      email,
      passwordHash,
      role
    ) <> ((User.apply _).tupled, User.unapply)

}
