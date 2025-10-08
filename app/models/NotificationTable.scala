package app.models

import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

class NotificationTable(tag: Tag) extends Table[Notification](tag, "notifications") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def expenseId = column[String]("expense_id")
  def toUser = column[String]("recipient")
  def message = column[String]("message")
  def createdAt = column[java.sql.Timestamp]("created_at")

  def * : ProvenShape[Notification] =
    (id.?, expenseId, toUser, message) <> ({ case (id, eId, to, msg) => Notification(id, eId, to, msg) },
    (n: Notification) => Some((n.id, n.expenseId, n.to, n.message)))
}
