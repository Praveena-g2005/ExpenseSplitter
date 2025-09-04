package app.models

import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

// Balance case class
case class Balance(
  id: Option[Long] = None,
  from: String,   // who owes
  to: String,     // whom they owe
  amount: Double
)

//JSON formatter
object Balance {
  implicit val format: OFormat[Balance] = Json.format[Balance]
}

//Slick table mapping
class BalanceTable(tag: Tag) extends Table[Balance](tag, "balances") {

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def from: Rep[String] = column[String]("from_user")   // rename in DB
  def to: Rep[String] = column[String]("to_user")   // rename in DB
  def amount: Rep[Double] = column[Double]("amount")

  def * : ProvenShape[Balance] =
    (id.?, from, to, amount) <> (Balance.tupled, Balance.unapply)
}
