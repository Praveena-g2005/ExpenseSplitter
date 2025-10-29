package app.models

import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}
import java.sql.Timestamp
import java.time.LocalDateTime

case class RefreshToken(
    id: Option[Long] = None,
    userId: Long,
    token: String,
    expiresAt: Timestamp,  
    revoked: Boolean = false
)

object RefreshToken {
  // Custom JSON formatter for Timestamp
  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsNumber(millis) => JsSuccess(new Timestamp(millis.toLong))
      case JsString(str) => JsSuccess(Timestamp.valueOf(str))
      case _ => JsError("Expected timestamp as number or string")
    }
    def writes(ts: Timestamp): JsValue = JsNumber(ts.getTime)
  }
  implicit val format: OFormat[RefreshToken] = Json.format[RefreshToken]
}

class RefreshTokenTable(tag: Tag) extends Table[RefreshToken](tag, "refresh_tokens") {
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId: Rep[Long] = column[Long]("user_id")
  def token: Rep[String] = column[String]("token")
  def expiresAt: Rep[Timestamp] = column[Timestamp]("expires_at")  
  def revoked: Rep[Boolean] = column[Boolean]("revoked")

  def * : ProvenShape[RefreshToken] =
    (id.?, userId, token, expiresAt, revoked) <> (RefreshToken.tupled, RefreshToken.unapply)
}