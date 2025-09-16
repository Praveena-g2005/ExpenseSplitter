package app.controllers

import app.services.{ExpenseService, ParticipantShare, ExpenseServiceFormats}
import app.models.Expense
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging

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
  implicit val format: OFormat[BalanceRelationshipResponse] = Json.format[BalanceRelationshipResponse]
}

case class ExpenseErrorResponse(
  success: Boolean,
  error: String
)

object ExpenseErrorResponse {
  implicit val format: OFormat[ExpenseErrorResponse] = Json.format[ExpenseErrorResponse]
}

// Response for delete action
case class SimpleResponse(success: Boolean, message: String)
object SimpleResponse {
  implicit val format: OFormat[SimpleResponse] = Json.format[SimpleResponse]
}

// JSON formatters
object CreateExpenseRequest {
  implicit val participantShareFormat: OFormat[ParticipantShare] = Json.format[ParticipantShare]
  implicit val format: OFormat[CreateExpenseRequest] = Json.format[CreateExpenseRequest]
}

object CreateExpenseResponse {
  implicit val format: OFormat[CreateExpenseResponse] = Json.format[CreateExpenseResponse]
}

object ExpenseListResponse {
  implicit val format: OFormat[ExpenseListResponse] = Json.format[ExpenseListResponse]
}

@Singleton
class ExpenseController @Inject()(
  cc: ControllerComponents,
  expenseService: ExpenseService
)(implicit ec: ExecutionContext) extends AbstractController(cc) with Logging {
  import ExpenseServiceFormats._

  def createExpense(): Action[JsValue] = Action.async(parse.json) { request =>
    logger.info(s"Creating expense with request: ${request.body}")

    request.body.validate[CreateExpenseRequest] match {
      case JsSuccess(expenseRequest, _) =>
        if (expenseRequest.description.trim.isEmpty) {
          Future.successful(BadRequest(Json.toJson(ExpenseErrorResponse(false, "Description cannot be empty"))))
        } else if (expenseRequest.amount <= 0) {
          Future.successful(BadRequest(Json.toJson(ExpenseErrorResponse(false, "Amount must be greater than 0"))))
        } else if (expenseRequest.participants.isEmpty) {
          Future.successful(BadRequest(Json.toJson(ExpenseErrorResponse(false, "At least one participant is required"))))
        } else {
          expenseService.createExpense(
            description = expenseRequest.description,
            amount = expenseRequest.amount,
            paidBy = expenseRequest.paidBy,
            participants = expenseRequest.participants
          ).map { result =>
            logger.info(s"Expense created successfully: ${result.expense.id}")
            Created(Json.toJson(CreateExpenseResponse(
              success = true,
              message = "Expense created successfully",
              expense = Some(result.expense),
              balanceCount = Some(result.balances.length)
            )))
          }.recover {
            case ex: IllegalArgumentException =>
              logger.warn(s"Validation error creating expense: ${ex.getMessage}")
              BadRequest(Json.toJson(ExpenseErrorResponse(false, ex.getMessage)))
            case ex: Exception =>
              logger.error(s"Error creating expense: ${ex.getMessage}", ex)
              InternalServerError(Json.toJson(ExpenseErrorResponse(false, "Internal server error")))
          }
        }

      case JsError(errors) =>
        logger.warn(s"Invalid JSON format: $errors")
        Future.successful(BadRequest(Json.toJson(ExpenseErrorResponse(false, "Invalid JSON format"))))
    }
  }

  def getAllExpenses(): Action[AnyContent] = Action.async {
    logger.info("Fetching all expenses")
    expenseService.getAllExpenses().map { expenses =>
      Ok(Json.toJson(ExpenseListResponse(
        expenses = expenses,
        count = expenses.length
      )))
    }.recover {
      case ex: Exception =>
        logger.error(s"Error fetching expenses: ${ex.getMessage}", ex)
        InternalServerError(Json.toJson(ExpenseErrorResponse(false, "Error fetching expenses")))
    }
  }

  def getExpenseById(id: Long): Action[AnyContent] = Action.async {
    logger.info(s"Fetching expense details for ID: $id")
    expenseService.getExpenseDetails(id).map {
      case Some(expenseDetails) =>
        Ok(Json.toJson(expenseDetails))
      case None =>
        NotFound(Json.toJson(ExpenseErrorResponse(false, s"Expense with ID $id not found")))
    }.recover {
      case ex: Exception =>
        logger.error(s"Error fetching expense $id: ${ex.getMessage}", ex)
        InternalServerError(Json.toJson(ExpenseErrorResponse(false, "Error fetching expense details")))
    }
  }

  def getExpensesByUser(userId: Long): Action[AnyContent] = Action.async {
    logger.info(s"Fetching expenses for user: $userId")
    expenseService.getUserExpenses(userId).map { expenses =>
      Ok(Json.toJson(ExpenseListResponse(
        expenses = expenses,
        count = expenses.length
      )))
    }.recover {
      case ex: Exception =>
        logger.error(s"Error fetching expenses for user $userId: ${ex.getMessage}", ex)
        InternalServerError(Json.toJson(ExpenseErrorResponse(false, "Error fetching user expenses")))
    }
  }

  def deleteExpense(id: Long): Action[AnyContent] = Action.async {
    logger.info(s"Deleting expense: $id")
    expenseService.deleteExpense(id).map { success =>
      if (success) {
        Ok(Json.toJson(SimpleResponse(true, s"Expense $id deleted successfully")))
      } else {
        NotFound(Json.toJson(ExpenseErrorResponse(false, s"Expense with ID $id not found")))
      }
    }.recover {
      case ex: Exception =>
        logger.error(s"Error deleting expense $id: ${ex.getMessage}", ex)
        InternalServerError(Json.toJson(ExpenseErrorResponse(false, "Error deleting expense")))
    }
  }
}
