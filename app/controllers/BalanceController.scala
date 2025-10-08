package app.controllers

import app.services.BalanceService
import app.models.Balance
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import app.dtos.{BalanceErrorResponse, BalanceListResponse, UserBalanceResponse}
import app.utils.{AuthAction, AuthenticatedRequest} // Add this

@Singleton
class BalanceController @Inject() (
  cc: ControllerComponents,
  balanceService: BalanceService,
  authAction: AuthAction // Add this
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with Logging {

  // GET /balances - Protected
  def getAllBalances(): Action[AnyContent] = authAction.async { request: AuthenticatedRequest[AnyContent] =>
    logger.info(s"Fetching all balances for user: ${request.user.id}")

    // Only return balances involving this user
    val userId = request.user.id.get
    for {
      owingBalances <- balanceService.getUnsettledBalances(userId)
      owedBalances <- balanceService.getIncomingBalances(userId)
    } yield {
      val allUserBalances = (owingBalances ++ owedBalances).distinctBy(_.id)
      Ok(
        Json.toJson(
          BalanceListResponse(
            balances = allUserBalances,
            count = allUserBalances.length
          )
        )
      )
    }
  }

  // GET /balances/user/:userId - Protected
  def getUserBalances(userId: Long): Action[AnyContent] = authAction.async {
    request: AuthenticatedRequest[AnyContent] =>
      logger.info(s"Fetching balance summary for user: $userId")

      // Authorization: Users can only view their own balances
      if (userId != request.user.id.get) {
        Future.successful(
          Forbidden(
            Json.toJson(
              BalanceErrorResponse(false, "You can only view your own balances")
            )
          )
        )
      } else {
        balanceService
          .getUserBalanceSummary(userId)
          .map { summary =>
            Ok(
              Json.toJson(
                UserBalanceResponse(
                  userId = userId,
                  totalOwedByUser = summary.amountOwing,
                  totalOwedToUser = summary.amountOwed,
                  netBalance = summary.netBalance,
                  relationships = List()
                )
              )
            )
          }
          .recover {
            case ex: Exception =>
              logger.error(
                s"Error fetching balance summary for user $userId: ${ex.getMessage}",
                ex
              )
              InternalServerError(
                Json.toJson(
                  BalanceErrorResponse(
                    false,
                    "Error fetching user balance summary"
                  )
                )
              )
          }
      }
  }

  // GET /balances/user/:userId/owes - Protected
  def getUserOwingBalances(userId: Long): Action[AnyContent] =
    authAction.async { request: AuthenticatedRequest[AnyContent] =>
      logger.info(s"Fetching outgoing balances for user: $userId")

      if (userId != request.user.id.get) {
        Future.successful(
          Forbidden(
            Json.toJson(
              BalanceErrorResponse(false, "You can only view your own balances")
            )
          )
        )
      } else {
        balanceService
          .getUnsettledBalances(userId)
          .map { balances =>
            Ok(
              Json.toJson(
                BalanceListResponse(
                  balances = balances,
                  count = balances.length
                )
              )
            )
          }
          .recover {
            case ex: Exception =>
              logger.error(
                s"Error fetching owing balances for user $userId: ${ex.getMessage}",
                ex
              )
              InternalServerError(
                Json.toJson(
                  BalanceErrorResponse(false, "Error fetching owing balances")
                )
              )
          }
      }
    }

  // GET /balances/user/:userId/owed - Protected
  def getUserOwedBalances(userId: Long): Action[AnyContent] = authAction.async {
    request: AuthenticatedRequest[AnyContent] =>
      logger.info(s"Fetching incoming balances for user: $userId")

      if (userId != request.user.id.get) {
        Future.successful(
          Forbidden(
            Json.toJson(
              BalanceErrorResponse(false, "You can only view your own balances")
            )
          )
        )
      } else {
        balanceService
          .getIncomingBalances(userId)
          .map { balances =>
            Ok(
              Json.toJson(
                BalanceListResponse(
                  balances = balances,
                  count = balances.length
                )
              )
            )
          }
          .recover {
            case ex: Exception =>
              logger.error(
                s"Error fetching owed balances for user $userId: ${ex.getMessage}",
                ex
              )
              InternalServerError(
                Json.toJson(
                  BalanceErrorResponse(false, "Error fetching owed balances")
                )
              )
          }
      }
  }

  // GET /balances/expense/:expenseId - Protected
  def getExpenseBalances(expenseId: Long): Action[AnyContent] =
    authAction.async { request: AuthenticatedRequest[AnyContent] =>
      logger.info(s"Fetching balances for expense: $expenseId")

      balanceService
        .getExpenseBalances(expenseId)
        .flatMap { balances =>
          if (balances.isEmpty) {
            Future.successful(
              NotFound(
                Json.toJson(
                  BalanceErrorResponse(
                    false,
                    s"No balances found for expense $expenseId"
                  )
                )
              )
            )
          } else {
            // Authorization: Check if user is involved in any of these balances
            val userId = request.user.id.get
            val isInvolved =
              balances.exists(b => b.from == userId || b.to == userId)

            if (isInvolved) {
              Future.successful(
                Ok(
                  Json.toJson(
                    BalanceListResponse(
                      balances = balances,
                      count = balances.length
                    )
                  )
                )
              )
            } else {
              Future.successful(
                Forbidden(
                  Json.toJson(
                    BalanceErrorResponse(
                      false,
                      "You don't have access to these balances"
                    )
                  )
                )
              )
            }
          }
        }
        .recover {
          case ex: Exception =>
            logger.error(
              s"Error fetching balances for expense $expenseId: ${ex.getMessage}",
              ex
            )
            InternalServerError(
              Json.toJson(
                BalanceErrorResponse(false, "Error fetching expense balances")
              )
            )
        }
    }
}
