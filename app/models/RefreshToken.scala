package app.models

import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}
import java.time.LocalDateTime

case class RefreshToken(
    id: Option[Long] = None,
    userId: Long,
    token: String,
    expiresAt: LocalDateTime,
    revoked: Boolean = false
)

object RefreshToken {
  implicit val format: OFormat[RefreshToken] = Json.format[RefreshToken]
}

class RefreshTokenTable(tag: Tag)
    extends Table[RefreshToken](tag, "refresh_tokens") {
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId: Rep[Long] = column[Long]("user_id")
  def token: Rep[String] = column[String]("token")
  def expiresAt: Rep[LocalDateTime] = column[LocalDateTime]("expires_at")
  def revoked: Rep[Boolean] = column[Boolean]("revoked")

  def * : ProvenShape[RefreshToken] =
    (
      id.?,
      userId,
      token,
      expiresAt,
      revoked
    ) <> (RefreshToken.tupled, RefreshToken.unapply)
}
