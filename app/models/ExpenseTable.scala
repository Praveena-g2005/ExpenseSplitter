package app.models

import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

class ExpenseTable(tag: Tag) extends Table[Expense](tag, "expenses") {

  // Columns
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def paidBy: Rep[String] = column[String]("paid_by")
  def amount: Rep[Double] = column[Double]("amount")
  def participants: Rep[String] = column[String]("participants") // store as CSV

  // Projection - case class mapping
  def * : ProvenShape[Expense] =
    (id.?, paidBy, amount, participants).<>(
      { case (id, paidBy, amount, participants) =>
          Expense(id, paidBy, amount, participants.split(",").toList)
      },
      { expense: Expense =>
          Some((expense.id, expense.paidBy, expense.amount, expense.participants.mkString(",")))
      }
    )
}
