package app.models

import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}
import app.models.Expense
import app.models.UserTable
class ExpenseTable(tag: Tag) extends Table[Expense](tag, "expenses") {

  // Columns
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def description: Rep[String] =column[String]("description")
  def amount: Rep[Double] = column[Double]("amount")
  def paidBy: Rep[Long] = column[Long]("paid_by")
  def fk_paidby = foreignKey("fk_paidby",paidBy,TableQuery[UserTable])(_.id)

  def *:ProvenShape[Expense]=
    (id.?,description,amount,paidBy)<>(Expense.tupled,Expense.unapply)
}
