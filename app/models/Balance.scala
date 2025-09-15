package app.models

import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}
import app.models.UserTable
import app.models.ExpenseTable
// Balance case class
case class Balance(
  id: Option[Long] = None,
  from: Long,   // who owes
  to: Long,    // whom they owe
  amount: Double,
  expenseId: Long
)

//JSON formatter
object Balance {
  implicit val format: OFormat[Balance] = Json.format[Balance]
}

//Slick table mapping
class BalanceTable(tag: Tag) extends Table[Balance](tag, "balances") {

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def from: Rep[Long] = column[Long]("from_user")   // rename in DB
  def to: Rep[Long] = column[Long]("to_user")   // rename in DB
  def amount: Rep[Double] = column[Double]("amount")
  def expenseId: Rep[Long] = column[Long]("expense_id")
  def fromUserFk = foreignKey("fk_from_user", from, TableQuery[UserTable])(_.id)
  def toUserFk = foreignKey("fk_to_user", to, TableQuery[UserTable])(_.id)
  def expenseFk = foreignKey("fk_expense", expenseId, TableQuery[ExpenseTable])(_.id)
  def * : ProvenShape[Balance] =
    (id.?, from, to, amount, expenseId) <> (Balance.tupled, Balance.unapply)
}
