
package app.repositories

import app.models.{Expense, ExpenseTable}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExpenseRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[MySQLProfile]
  private val db = dbConfig.db

  private val expenses = TableQuery[ExpenseTable]

  def all(): Future[Seq[Expense]] =
    db.run(expenses.result)

  def create(expense: Expense): Future[Long] = {
    val insertQuery = (expenses returning expenses.map(_.id)) += expense
    db.run(insertQuery)
  }

  def findById(id: Long): Future[Option[Expense]] =
    db.run(expenses.filter(_.id === id).result.headOption)
}
