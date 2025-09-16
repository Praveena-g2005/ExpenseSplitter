package app.controllers

import app.services.BalanceService
import app.models.Balance
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging

// Response case classes
case class BalanceListResponse(
  balances: List[Balance],
  count: Int
)

case class UserBalanceResponse(
  userId: Long,
  totalOwedByUser: Double,  // What user owes to others
  totalOwedToUser: Double,  // What others owe to user
  netBalance: Double,       // Positive = user is owed money, Negative = user owes money
  relationships: List[BalanceRelationshipResponse]
)

case class ErrorResponse(
  success: Boolean,
  error: String
)

// JSON formatters
object BalanceListResponse {
  implicit val format: OFormat[BalanceListResponse] = Json.format[BalanceListResponse]
}

object UserBalanceResponse {
  implicit val format: OFormat[UserBalanceResponse] = Json.format[UserBalanceResponse]
}

object ErrorResponse {
  implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

@Singleton
class BalanceController @Inject()(
  cc: ControllerComponents,
  balanceService: BalanceService
)(implicit ec: ExecutionContext) extends AbstractController(cc) with Logging {

  /**
   * GET /balances
   */
  def getAllBalances(): Action[AnyContent] = Action.async {
    logger.info("Fetching all balances")
    
    balanceService.getAllBalances().map { balances =>
      Ok(Json.toJson(BalanceListResponse(
        balances = balances,
        count = balances.length
      )))
    }.recover {
      case ex: Exception =>
        logger.error(s"Error fetching balances: ${ex.getMessage}", ex)
        InternalServerError(Json.toJson(ErrorResponse(false, "Error fetching balances")))
    }
  }

  /**
   * GET /balances/user/:userId
   * Get complete balance summary for a user - who they owe and who owes them
   */
def getUserBalances(userId: Long): Action[AnyContent] = Action.async {
  logger.info(s"Fetching balance summary for user: $userId")
  
  balanceService.getUserBalanceSummary(userId).map { summary =>
    Ok(Json.toJson(UserBalanceResponse(
      userId = userId,
      totalOwedByUser = summary.amountOwing,
      totalOwedToUser = summary.amountOwed,
      netBalance = summary.netBalance,
      relationships = List() 
    )))
  }.recover {
    case ex: Exception =>
      logger.error(s"Error fetching balance summary for user $userId: ${ex.getMessage}", ex)
      InternalServerError(Json.toJson(ErrorResponse(false, "Error fetching user balance summary")))
  }
}

  /**
   * GET /balances/user/:userId/owes
   * Get what this user owes to others (outgoing balances)
   */
  def getUserOwingBalances(userId: Long): Action[AnyContent] = Action.async {
    logger.info(s"Fetching outgoing balances for user: $userId")
    
    balanceService.getUnsettledBalances(userId).map { balances =>
      Ok(Json.toJson(BalanceListResponse(
        balances = balances,
        count = balances.length
      )))
    }.recover {
      case ex: Exception =>
        logger.error(s"Error fetching owing balances for user $userId: ${ex.getMessage}", ex)
        InternalServerError(Json.toJson(ErrorResponse(false, "Error fetching owing balances")))
    }
  }

  /**
   * GET /balances/user/:userId/owed
   * Get what others owe to this user (incoming balances)
   */
  def getUserOwedBalances(userId: Long): Action[AnyContent] = Action.async {
    logger.info(s"Fetching incoming balances for user: $userId")
    
    balanceService.getIncomingBalances(userId).map { balances =>
      Ok(Json.toJson(BalanceListResponse(
        balances = balances,
        count = balances.length
      )))
    }.recover {
      case ex: Exception =>
        logger.error(s"Error fetching owed balances for user $userId: ${ex.getMessage}", ex)
        InternalServerError(Json.toJson(ErrorResponse(false, "Error fetching owed balances")))
    }
  }

  /**
   * GET /balances/expense/:expenseId
   * Get all balances created from a specific expense
   */
  def getExpenseBalances(expenseId: Long): Action[AnyContent] = Action.async {
    logger.info(s"Fetching balances for expense: $expenseId")
    
    balanceService.getExpenseBalances(expenseId).map { balances =>
      Ok(Json.toJson(BalanceListResponse(
        balances = balances,
        count = balances.length
      )))
    }.recover {
      case ex: Exception =>
        logger.error(s"Error fetching balances for expense $expenseId: ${ex.getMessage}", ex)
        InternalServerError(Json.toJson(ErrorResponse(false, "Error fetching expense balances")))
    }
  }
}