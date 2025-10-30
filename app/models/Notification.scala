package app.models

import play.api.libs.json._
import java.sql.Timestamp

case class Notification(
  id: Option[Long] = None,
  expenseId: String,
  to: String,
  message: String,
  createdAt: Option[Timestamp] = None
)

object Notification {
  // JSON formatter for java.sql.Timestamp
  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def writes(ts: Timestamp): JsValue = JsString(ts.toString)
    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(s) =>
        try JsSuccess(Timestamp.valueOf(s))
        catch { case _: Exception => JsError("Invalid timestamp format") }
      case _ => JsError("String value expected for timestamp")
    }
  }

  // JSON formatter for Notification
  implicit val format: OFormat[Notification] = Json.format[Notification]
}
