package app.repositories

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import app.models.{Notification, NotificationTable}
import java.sql.Timestamp
import java.time.Instant

@Singleton
class NotificationRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[MySQLProfile]
  private val db = dbConfig.db
  private val notifications = TableQuery[NotificationTable]

  def create(n: Notification): Future[Long] = {
    val notificationWithTs =
      n.copy(createdAt = Some(Timestamp.from(Instant.now())))
    val insertQuery = (notifications.map(t => (t.id.?, t.expenseId, t.toUser, t.message, t.createdAt))
      returning notifications.map(_.id)) += (
      notificationWithTs.id,
      notificationWithTs.expenseId,
      notificationWithTs.to,
      notificationWithTs.message,
      notificationWithTs.createdAt.get
    )
    db.run(insertQuery)
  }
}
