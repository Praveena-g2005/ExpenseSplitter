package app.dtos

import play.api.libs.json._
import app.models.{Balance, Expense}
import app.services.{ExpenseService, ExpenseServiceFormats, ParticipantShare}
// JSON request/response case classes
case class CreateExpenseRequest(
    description: String,
    amount: Double,
    paidBy: Long,
    participants: List[ParticipantShare]
)

case class CreateExpenseResponse(
    success: Boolean,
    message: String,
    expense: Option[Expense] = None,
    balanceCount: Option[Int] = None
)

case class ExpenseListResponse(
    expenses: List[Expense],
    count: Int
)

case class BalanceRelationshipResponse(
    otherUserId: Long,
    otherUserName: String,
    otherUserEmail: String,
    netAmount: Double
)

object BalanceRelationshipResponse {
  implicit val format: OFormat[BalanceRelationshipResponse] =
    Json.format[BalanceRelationshipResponse]
}

case class ExpenseErrorResponse(
    success: Boolean,
    error: String
)

object ExpenseErrorResponse {
  implicit val format: OFormat[ExpenseErrorResponse] =
    Json.format[ExpenseErrorResponse]
}

// Response for delete action
case class SimpleResponse(success: Boolean, message: String)
object SimpleResponse {
  implicit val format: OFormat[SimpleResponse] = Json.format[SimpleResponse]
}

// JSON formatters
object CreateExpenseRequest {
  implicit val participantShareFormat: OFormat[ParticipantShare] =
    Json.format[ParticipantShare]
  implicit val format: OFormat[CreateExpenseRequest] =
    Json.format[CreateExpenseRequest]
}

object CreateExpenseResponse {
  implicit val format: OFormat[CreateExpenseResponse] =
    Json.format[CreateExpenseResponse]
}

object ExpenseListResponse {
  implicit val format: OFormat[ExpenseListResponse] =
    Json.format[ExpenseListResponse]
}
