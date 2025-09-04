package app.models

import play.api.libs.json._

case class Expense(
                    id: Option[Long] = None,
                    paidBy: String,
                    amount: Double,
                    participants: List[String]
                  )

object Expense {
  implicit val format: OFormat[Expense] = Json.format[Expense]
}
