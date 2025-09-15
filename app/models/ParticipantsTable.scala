package app.models

import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}
import app.models.{ExpenseTable,UserTable}

case class Participants(
    id: Option[Long] =None,
    expenseid :Long,
    userid :Long,
    sharedamt :Double
)

object Participants{
    implicit val format :OFormat[Participants] =Json.format[Participants]
}

class ParticipantsTable (tag : Tag) extends Table[Participants](tag, "expense_participants"){
    def id: Rep[Long] = column[Long]("id",O.PrimaryKey,O.AutoInc)
    def expenseid : Rep[Long] = column[Long]("expense_id")
    def userid: Rep[Long] = column[Long]("user_id")
    def sharedamt : Rep[Double] = column[Double]("shared_amt")
    def fk_expense = foreignKey("fk_expense", expenseid,TableQuery[ExpenseTable])(_.id)
    def fk_user_id = foreignKey("fk_user", userid, TableQuery[UserTable])(_.id)

    def * : ProvenShape[Participants]=
        (id.?,expenseid,userid,sharedamt)<>(Participants.tupled,Participants.unapply)   
}
