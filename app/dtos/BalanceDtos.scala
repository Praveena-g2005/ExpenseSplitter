package app.dtos

import play.api.libs.json._
import app.models.Balance

case class BalanceListResponse(
  balances: List[Balance],
  count: Int
)

case class UserBalanceResponse(
  userId: Long,
  totalOwedByUser: Double, // What user owes to others
  totalOwedToUser: Double, // What others owe to user
  netBalance: Double, // Positive = user is owed money, Negative = user owes money
  relationships: List[BalanceRelationshipResponse]
)

case class BalanceErrorResponse(
  success: Boolean,
  error: String
)

// JSON formatters
object BalanceListResponse {
  implicit val format: OFormat[BalanceListResponse] =
    Json.format[BalanceListResponse]
}

object UserBalanceResponse {
  implicit val format: OFormat[UserBalanceResponse] =
    Json.format[UserBalanceResponse]
}

object BalanceErrorResponse {
  implicit val format: OFormat[BalanceErrorResponse] =
    Json.format[BalanceErrorResponse]
}
