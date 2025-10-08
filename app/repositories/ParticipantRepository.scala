package app.repositories

import app.models.{Expense, Participants, ParticipantsTable}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ParticipantRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {
  private val db = dbConfigProvider.get.db
  private val participants = TableQuery[ParticipantsTable]

  def create(participant: Participants): Future[Participants] = {
    val participantwithoutid = participant.copy(id = None)
    val insertQuery =
      participants returning participants.map(_.id) += participantwithoutid
    db.run(insertQuery).map { generatedId =>
      participant.copy(id = Some(generatedId))
    }
  }
  def findByExpenseId(id: Long): Future[List[Participants]] =
    db.run(participants.filter(_.expenseid === id).result).map(_.toList)
  def findByUser(userid: Long): Future[List[Participants]] =
    db.run(participants.filter(_.userid === userid).result).map(_.toList)
  def deleteByExpenseId(expenseid: Long): Future[Int] =
    db.run(participants.filter(_.expenseid === expenseid).delete)
}
