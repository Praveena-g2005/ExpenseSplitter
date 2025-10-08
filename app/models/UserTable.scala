package app.models

import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

case class User(
    id: Option[Long] = None,
    name: String,
    email: String,
    passwordHash: String
)
object User {
  implicit val Format: OFormat[User] = Json.format[User]
}

class UserTable(tag: Tag) extends Table[User](tag, "users") {
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name: Rep[String] = column[String]("name")
  def email: Rep[String] = column[String]("email")
  def passwordHash: Rep[String] = column[String]("password_hash")
  def * : ProvenShape[User] =
    (id.?, name, email, passwordHash) <> (User.tupled, User.unapply)
}
