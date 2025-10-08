package app.services

import play.api.libs.json._
import app.models.{Balance, User}
import app.repositories.{BalanceRepository, UserRepository}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class UserBalanceSummary(
    userId: Long,
    userName: String,
    userEmail: String,
    amountOwed: Double,
    amountOwing: Double,
    netBalance: Double
)
object UserBalanceSummary {
  implicit val format: OFormat[UserBalanceSummary] =
    Json.format[UserBalanceSummary]
}
@Singleton
class BalanceService @Inject() (
    balanceRepository: BalanceRepository,
    userRepository: UserRepository
)(implicit ec: ExecutionContext) {

  /** Get user's balance summary - who they owe and who owes them
    */
  def getUserBalanceSummary(userId: Long): Future[UserBalanceSummary] = {
    for {
      user <- userRepository.findById(userId)
      totalOwed <- balanceRepository.getTotalOwnedTo(userId)
      totalOwing <- balanceRepository.getTotalOwnedBy(userId)
    } yield UserBalanceSummary(
      userId = userId,
      userName = user.map(_.name).getOrElse("Unknown"),
      userEmail = user.map(_.email).getOrElse(""),
      amountOwed = totalOwed,
      amountOwing = totalOwing,
      netBalance = totalOwed - totalOwing
    )
  }

  def getAllBalances(): Future[List[Balance]] = {
    balanceRepository.findAll()
  }

  /** Get balances that need to be settled (for dashboard)
    */
  def getUnsettledBalances(userId: Long): Future[List[Balance]] = {
    balanceRepository.findByFromUser(userId)
  }

  /** Get balances where others owe money to this user
    */
  def getIncomingBalances(userId: Long): Future[List[Balance]] = {
    balanceRepository.findByToUser(userId)
  }

  /** Settle a balance between users (simplified version) In a real app, this
    * would create settlement records
    */
  def settleBalance(
      fromUserId: Long,
      toUserId: Long,
      amount: Double
  ): Future[Boolean] = {
    // This is a simplified implementation
    // In production, you'd create settlement records and update balance status
    Future.successful(true) // Placeholder
  }

  /** Get all balances for an expense (useful for expense details page)
    */
  def getExpenseBalances(expenseId: Long): Future[List[Balance]] = {
    balanceRepository.findByExpenseId(expenseId)
  }
}
