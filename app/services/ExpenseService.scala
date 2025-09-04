package app.services

import javax.inject._
import app.models.{Expense, Balance}
import app.repositories.{ExpenseRepository, BalanceRepository}
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable
import app.grpc.NotificationClient
import notification.notification.{NotificationServiceGrpc, NotifyRequest, NotifyResponse, NotifyManyRequest}
@Singleton
class ExpenseService @Inject()(
  expenseRepo: ExpenseRepository,
  balanceRepo: BalanceRepository,
  notificationClient: NotificationClient 
)(implicit ec: ExecutionContext) {

  /** Add a new expense (writes to DB) */
def addExpense(expense: Expense): Future[Expense] = {
  // Step 1: Save expense in DB
  expenseRepo.create(expense).flatMap { id =>
    val created = expense.copy(id = Some(id))

    // Step 2: Prepare notification
    val recipients = created.participants.filterNot(_ == created.paidBy)
    val msg = s"${created.paidBy} paid ₹${created.amount} split among ${created.participants.mkString(", ")}"

    // Step 3: Fire-and-forget gRPC notification
    notificationClient.notifyMany(      
  id,          // Long
  recipients,  // Seq[String]
  msg          // String
    ).recover { case e =>
      println(s"[gRPC] Notify failed: ${e.getMessage}")
      NotifyResponse(ok = false, details = e.getMessage)
    }

    // Step 4: Return the created expense
    Future.successful(created)
  }
}


  // def addExpense(expense: Expense): Future[Expense] = {
  //   expenseRepo.create(expense).map { id =>
  //     expense.copy(id = Some(id))
  //   }
  // }

  /** Add expense and update balances immediately */
  def addExpenseAndUpdateBalances(expense: Expense): Future[Seq[Balance]] = {
    addExpense(expense).flatMap { _ =>
      calculateBalances()
    }
  }

  /** Calculate balances for all participants (reads from DB) */
  def calculateBalances(): Future[Seq[Balance]] = {
    expenseRepo.all().flatMap { expenses =>
      if (expenses.isEmpty) Future.successful(Seq.empty)
      else {
        // Step 1: Collect all pairwise debts
        val debts = mutable.Map[(String, String), Double]().withDefaultValue(0.0)
        expenses.foreach { e =>
          val splitAmount = e.amount / e.participants.size
          e.participants.foreach { participant =>
            if (participant != e.paidBy) {
              debts((participant, e.paidBy)) += splitAmount
            }
          }
        }

        // Step 2: Net the debts
        val netDebts = mutable.Map[(String, String), Double]()
        val processedPairs = mutable.Set[(String, String)]()

        debts.foreach { case ((from, to), amount) =>
          if (from != to && !processedPairs.contains((from, to)) && !processedPairs.contains((to, from))) {
            val reverseAmount = debts.getOrElse((to, from), 0.0)
            val netAmount = amount - reverseAmount
            if (netAmount > 0) netDebts((from, to)) = netAmount
            else if (netAmount < 0) netDebts((to, from)) = -netAmount
            processedPairs += ((from, to))
            processedPairs += ((to, from))
          }
        }

        // Step 3: Convert to Balance objects
        val balances = netDebts.collect {
          case ((from, to), amount) if from != to && amount > 0 =>
            Balance(None, from, to, amount)
        }.toSeq

        // Step 4: Clear old balances and save new ones
        for {
          _ <- balanceRepo.clear()
          _ <- balanceRepo.saveAll(balances)
        } yield balances
      }
    }
  }
}
