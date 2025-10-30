package app.services

import play.api.libs.json._
import app.models.{Balance, Expense, Participants, UserTable}
import app.repositories.{BalanceRepository, ExpenseRepository, ParticipantRepository, UserRepository}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import app.models.User

// Supporting case classes
case class ParticipantShare(userId: Long, shareAmount: Double)
case class ExpenseCreationResult(expense: Expense, balances: List[Balance])
case class ExpenseDetails(
  expense: Expense,
  participants: List[Participants],
  balances: List[Balance],
  participantUsers: List[User]
)
case class UserExpenseStats(
  totalPaid: Double,
  expenseCount: Int,
  totalOwedByUser: Double,
  totalOwedToUser: Double,
  netBalance: Double
)

object ExpenseServiceFormats {
  implicit val userTableFormat: OFormat[User] = Json.format[User]
  implicit val participantShareFormat: OFormat[ParticipantShare] =
    Json.format[ParticipantShare]
  implicit val expenseDetailsFormat: OFormat[ExpenseDetails] =
    Json.format[ExpenseDetails]
}

@Singleton
class ExpenseService @Inject() (
  expenseRepository: ExpenseRepository,
  participantRepository: ParticipantRepository,
  balanceRepository: BalanceRepository,
  userRepository: UserRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  import ExpenseServiceFormats._

  /** Creates an expense and calculates balances */
  def createExpense(
    description: String,
    amount: Double,
    paidBy: Long,
    participants: List[ParticipantShare]
  ): Future[ExpenseCreationResult] = {

    val totalShares = participants.map(_.shareAmount).sum
    if (Math.abs(totalShares - amount) > 0.01) {
      Future.failed(
        new IllegalArgumentException(
          s"Participant shares ($totalShares) don't match expense amount ($amount)"
        )
      )
    } else {
      createExpenseTransaction(description, amount, paidBy, participants)
    }
  }

  /** Helper method for equal split expenses */
  def createEqualSplitExpense(
    description: String,
    amount: Double,
    paidBy: Long,
    participantIds: List[Long]
  ): Future[ExpenseCreationResult] = {
    val sharePerPerson = amount / participantIds.length
    val participants =
      participantIds.map(id => ParticipantShare(id, sharePerPerson))
    createExpense(description, amount, paidBy, participants)
  }

  def getAllExpenses(): Future[List[Expense]] = expenseRepository.findAll()

  private def createExpenseTransaction(
    description: String,
    amount: Double,
    paidBy: Long,
    participants: List[ParticipantShare]
  ): Future[ExpenseCreationResult] = {

    val expense = Expense(
      id = None,
      description = description,
      amount = amount,
      paidBy = paidBy
    )

    for {
      createdExpense <- expenseRepository.create(expense)

      participantRecords = participants.map(p =>
        Participants(
          id = None,
          expenseid = createdExpense.id.get,
          userid = p.userId,
          sharedamt = p.shareAmount
        )
      )
      _ <- Future.sequence(participantRecords.map(participantRepository.create))

      balanceRecords = participants
        .filter(_.userId != paidBy)
        .map(p =>
          Balance(
            id = None,
            from = p.userId,
            to = paidBy,
            amount = p.shareAmount,
            expenseId = createdExpense.id.get
          )
        )

      createdBalances <- Future.sequence(
        balanceRecords.map(balanceRepository.create)
      )

    } yield ExpenseCreationResult(createdExpense, createdBalances)
  }

  /** Get all expenses for a user (paid by them or participated in) */
  def getUserExpenses(userId: Long): Future[List[Expense]] =
    for {
      paidExpenses <- expenseRepository.findByPaidBy(userId)
      participatedExpenses <- expenseRepository.findByParticipant(userId)
    } yield (paidExpenses ++ participatedExpenses).distinctBy(_.id)

  /** Delete an expense and all related data */
  def deleteExpense(expenseId: Long): Future[Boolean] =
    for {
      _ <- balanceRepository.deleteByExpenseId(expenseId)
      _ <- participantRepository.deleteByExpenseId(expenseId)
      deleteCount <- expenseRepository.deleteById(expenseId)
    } yield deleteCount > 0

  /** Get expense details with participants */
  def getExpenseDetails(expenseId: Long): Future[Option[ExpenseDetails]] =
    for {
      expenseOpt <- expenseRepository.findById(expenseId)
      expense <- expenseOpt match {
        case Some(exp) =>
          for {
            participants <- participantRepository.findByExpenseId(expenseId)
            balances <- balanceRepository.findByExpenseId(expenseId)
            participantUsers <- {
              val userIds = participants.map(_.userid)
              userRepository.findByIds(userIds)
            }
          } yield Some(
            ExpenseDetails(exp, participants, balances, participantUsers)
          )
        case None => Future.successful(None)
      }
    } yield expense

  /** Get user's expense statistics */
  def getUserExpenseStats(userId: Long): Future[UserExpenseStats] =
    for {
      totalPaid <- expenseRepository.getTotalPaidBy(userId)
      expenseCount <- expenseRepository.countByUser(userId)
      totalOwed <- balanceRepository.getTotalOwnedBy(userId)
      totalOwedTo <- balanceRepository.getTotalOwnedTo(userId)
    } yield UserExpenseStats(
      totalPaid = totalPaid,
      expenseCount = expenseCount,
      totalOwedByUser = totalOwed,
      totalOwedToUser = totalOwedTo,
      netBalance = totalOwedTo - totalOwed
    )
}
