package app.repositories

import app.models.{Balance, BalanceTable}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BalanceRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[MySQLProfile]
  private val db = dbConfig.db

  private val balances = TableQuery[BalanceTable]

  def all(): Future[Seq[Balance]] =
    db.run(balances.result)

  def saveAll(newBalances: Seq[Balance]): Future[Unit] = {
    db.run(balances ++= newBalances).map(_ => ())
  }

  def clear(): Future[Int] =
    db.run(balances.delete)
}
