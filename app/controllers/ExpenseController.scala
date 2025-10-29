package app.controllers

import app.services.{ExpenseService, ExpenseServiceFormats, ParticipantShare}
import app.models.{Expense, UserRole}
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import app.dtos.{
  CreateExpenseRequest,
  CreateExpenseResponse,
  ExpenseErrorResponse,
  ExpenseListResponse,
  SimpleResponse
}
import app.utils.{AuthAction, AuthenticatedRequest}

@Singleton
class ExpenseController @Inject() (
    cc: ControllerComponents,
    expenseService: ExpenseService,
    authAction: AuthAction
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with Logging {

  import ExpenseServiceFormats._

  // POST /expenses
  def createExpense(): Action[JsValue] = authAction.async(parse.json) {
    request: AuthenticatedRequest[JsValue] =>
      logger.info(s"Creating expense for user: ${request.user.id}")

      request.body.validate[CreateExpenseRequest] match {
        case JsSuccess(expenseRequest, _) =>
          if (expenseRequest.paidBy != request.user.id.get) {
            Future.successful(
              Forbidden(
                Json.toJson(
                  ExpenseErrorResponse(
                    false,
                    "You can only create expenses for yourself"
                  )
                )
              )
            )
          } else if (expenseRequest.description.trim.isEmpty) {
            Future.successful(
              BadRequest(
                Json.toJson(
                  ExpenseErrorResponse(false, "Description cannot be empty")
                )
              )
            )
          } else if (expenseRequest.amount <= 0) {
            Future.successful(
              BadRequest(
                Json.toJson(
                  ExpenseErrorResponse(false, "Amount must be greater than 0")
                )
              )
            )
          } else if (expenseRequest.participants.isEmpty) {
            Future.successful(
              BadRequest(
                Json.toJson(
                  ExpenseErrorResponse(
                    false,
                    "At least one participant is required"
                  )
                )
              )
            )
          } else {
            expenseService
              .createExpense(
                description = expenseRequest.description,
                amount = expenseRequest.amount,
                paidBy = expenseRequest.paidBy,
                participants = expenseRequest.participants
              )
              .map { result =>
                logger.info(
                  s"Expense created successfully: ${result.expense.id}"
                )
                Created(
                  Json.toJson(
                    CreateExpenseResponse(
                      success = true,
                      message = "Expense created successfully",
                      expense = Some(result.expense),
                      balanceCount = Some(result.balances.length)
                    )
                  )
                )
              }
              .recover {
                case ex: IllegalArgumentException =>
                  logger.warn(
                    s"Validation error creating expense: ${ex.getMessage}"
                  )
                  BadRequest(
                    Json.toJson(ExpenseErrorResponse(false, ex.getMessage))
                  )
                case ex: Exception =>
                  logger.error(s"Error creating expense: ${ex.getMessage}", ex)
                  InternalServerError(
                    Json.toJson(
                      ExpenseErrorResponse(false, "Internal server error")
                    )
                  )
              }
          }

        case JsError(errors) =>
          logger.warn(s"Invalid JSON format: $errors")
          Future.successful(
            BadRequest(
              Json.toJson(ExpenseErrorResponse(false, "Invalid JSON format"))
            )
          )
      }
  }

  // GET /expenses
  def getAllExpenses(): Action[AnyContent] = authAction.async { request =>
    expenseService
      .getUserExpenses(request.user.id.get)
      .map { expenses =>
        Ok(
          Json.toJson(
            ExpenseListResponse(expenses = expenses, count = expenses.length)
          )
        )
      }
      .recover { case ex: Exception =>
        logger.error(s"Error fetching expenses: ${ex.getMessage}", ex)
        InternalServerError(
          Json.toJson(ExpenseErrorResponse(false, "Error fetching expenses"))
        )
      }
  }

  // GET /expenses/:id
  def getExpenseById(id: Long): Action[AnyContent] = authAction.async {
    request =>
      expenseService
        .getExpenseDetails(id)
        .map {
          case Some(expenseDetails) =>
            val userId = request.user.id.get
            val isPayer = expenseDetails.expense.paidBy == userId
            val isParticipant =
              expenseDetails.participants.exists(_.userid == userId)

            if (isPayer || isParticipant)
              Ok(Json.toJson(expenseDetails))
            else
              Forbidden(
                Json.toJson(
                  ExpenseErrorResponse(
                    false,
                    "You don't have access to this expense"
                  )
                )
              )
          case None =>
            NotFound(
              Json.toJson(
                ExpenseErrorResponse(false, s"Expense with ID $id not found")
              )
            )
        }
        .recover { case ex: Exception =>
          logger.error(s"Error fetching expense $id: ${ex.getMessage}", ex)
          InternalServerError(
            Json.toJson(
              ExpenseErrorResponse(false, "Error fetching expense details")
            )
          )
        }
  }

  // GET /expenses/user/:userId
  def getExpensesByUser(userId: Long): Action[AnyContent] = authAction.async {
    request =>
      if (
        request.user.role != UserRole.ADMIN && userId != request.user.id.get
      ) {
        Future.successful(
          Forbidden(
            Json.toJson(
              ExpenseErrorResponse(false, "You can only view your own expenses")
            )
          )
        )
      } else {
        expenseService
          .getUserExpenses(userId)
          .map { expenses =>
            Ok(
              Json.toJson(
                ExpenseListResponse(
                  expenses = expenses,
                  count = expenses.length
                )
              )
            )
          }
          .recover { case ex: Exception =>
            logger.error(
              s"Error fetching expenses for user $userId: ${ex.getMessage}",
              ex
            )
            InternalServerError(
              Json.toJson(
                ExpenseErrorResponse(false, "Error fetching user expenses")
              )
            )
          }
      }
  }

  // DELETE /expenses/:id
  def deleteExpense(id: Long): Action[AnyContent] = authAction.async {
    request =>
      expenseService
        .getExpenseDetails(id)
        .flatMap {
          case Some(expenseDetails) =>
            if (expenseDetails.expense.paidBy == request.user.id.get) {
              expenseService.deleteExpense(id).map { success =>
                if (success)
                  Ok(
                    Json.toJson(
                      SimpleResponse(true, s"Expense $id deleted successfully")
                    )
                  )
                else
                  InternalServerError(
                    Json.toJson(
                      ExpenseErrorResponse(false, "Failed to delete expense")
                    )
                  )
              }
            } else {
              Future.successful(
                Forbidden(
                  Json.toJson(
                    ExpenseErrorResponse(
                      false,
                      "You can only delete your own expenses"
                    )
                  )
                )
              )
            }
          case None =>
            Future.successful(
              NotFound(
                Json.toJson(
                  ExpenseErrorResponse(false, s"Expense with ID $id not found")
                )
              )
            )
        }
        .recover { case ex: Exception =>
          logger.error(s"Error deleting expense $id: ${ex.getMessage}", ex)
          InternalServerError(
            Json.toJson(ExpenseErrorResponse(false, "Error deleting expense"))
          )
        }
  }
}
