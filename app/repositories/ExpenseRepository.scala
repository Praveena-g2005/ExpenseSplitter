
package app.repositories

import app.models.{Expense, ExpenseTable, Participants, ParticipantsTable}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExpenseRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {

  private val db=dbConfigProvider.get.db
  private val expenses = TableQuery[ExpenseTable]
  private val participants = TableQuery[ParticipantsTable]

  def findAll(): Future[List[Expense]] =
    db.run(expenses.result).map(_.toList)

  def create(expense: Expense): Future[Expense] = {
    val expensewithoutid =expense.copy(id=None)
    val insertQuery = expenses returning expenses.map(_.id) += expensewithoutid
    db.run(insertQuery).map {generatedId =>
      expense.copy(id=Some(generatedId))
    }
  }

  def findById(id: Long): Future[Option[Expense]] =
    db.run(expenses.filter(_.id === id).result.headOption)

  def deleteById(id :Long) :Future[Int] =
    db.run(expenses.filter(_.id=== id).delete)
  
  def findByPaidBy (id : Long) :Future[List[Expense]] =
    db.run(expenses.filter(_.paidBy === id).result).map(_.toList)

  def findByParticipant (id : Long) :Future[List[Expense]] ={
    val expenseids = participants.filter(_.userid=== id).map(_.expenseid)
    db.run(expenses.filter(_.id in expenseids).result).map(_.toList)
  }
  def getTotalPaidBy(id :Long) :Future[Double]=
    db.run(expenses.filter(_.paidBy === id).map(_.amount).sum.result).map(_.getOrElse(0.0))

  def countByUser(id :Long) :Future[Int] =
    db.run(expenses.filter(_.paidBy ===id).length.result)
}