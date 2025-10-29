package app.models

import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}
import java.sql.Timestamp

case class RevokedToken(
    id: Option[Long] = None,
    token: String,
    userId: Long,
    tokenType: String,
    revokedAt: Timestamp,
    expiresAt: Timestamp
)

object RevokedToken {
  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsNumber(millis) => JsSuccess(new Timestamp(millis.toLong))
      case JsString(str)    => JsSuccess(Timestamp.valueOf(str))
      case _                => JsError("Expected timestamp as number or string")
    }
    def writes(ts: Timestamp): JsValue = JsNumber(ts.getTime)
  }

  implicit val format: OFormat[RevokedToken] = Json.format[RevokedToken]
}

class RevokedTokenTable(tag: Tag)
    extends Table[RevokedToken](tag, "revoked_tokens") {
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def token: Rep[String] = column[String]("token")
  def userId: Rep[Long] = column[Long]("user_id")
  def tokenType: Rep[String] = column[String]("token_type")
  def revokedAt: Rep[Timestamp] = column[Timestamp]("revoked_at")
  def expiresAt: Rep[Timestamp] = column[Timestamp]("expires_at")

  def userFk =
    foreignKey("fk_revoked_user", userId, TableQuery[UserTable])(_.id)
  def idx = index("idx_token", token)

  def * : ProvenShape[RevokedToken] =
    (
      id.?,
      token,
      userId,
      tokenType,
      revokedAt,
      expiresAt
    ) <> (RevokedToken.tupled, RevokedToken.unapply)
}
