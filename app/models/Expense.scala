package app.models

import play.api.libs.json._

case class Expense(
  id: Option[Long] = None,
  description: String,
  amount: Double,
  paidBy: Long
)

object Expense {
  implicit val format: OFormat[Expense] = Json.format[Expense]
}
