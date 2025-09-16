package app.repositories

import app.models.{Balance, BalanceTable}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BalanceRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  protected val db = dbConfigProvider.get.db
  private val balances = TableQuery[BalanceTable]

  def create(balance: Balance): Future[Balance] = {
    val balancewithoutid = balance.copy(id= None)
    val insertQuery= balances returning balances.map(_.id) +=balancewithoutid
    db.run(insertQuery).map{generatedId=>
      balance.copy(id=Some(generatedId))
    }
  }          // Add new balance entry
  def findByFromUser(userId: Long): Future[List[Balance]] = {
    db.run(balances.filter(_.from === userId).result).map(_.toList)
  }          // What user owes others
  def findByToUser(userId: Long): Future[List[Balance]] = {
    db.run(balances.filter(_.to === userId).result).map(_.toList)
  }          // What others owe user  
  def findByExpenseId(expenseId: Long): Future[List[Balance]] = {
    db.run(balances.filter(_.expenseId === expenseId).result).map(_.toList)
  }          // Balances for specific expense
  def updateAmount(balanceId: Long, amount: Double): Future[Int] = {
    val updateQuery = balances.filter(_.id=== balanceId).map(_.amount).update(amount)
    db.run(updateQuery)
  }          // Update balance amount
  def deleteByExpenseId(expenseId: Long): Future[Int] = {
    db.run(balances.filter(_.expenseId === expenseId).delete)
  }          // Delete when expense removed
  def getTotalOwnedTo(userid: Long) :Future[Double] ={
    db.run(balances.filter(_.to === userid).map(_.amount).sum.result).map(_.getOrElse(0.0))
  }
  def getTotalOwnedBy(userid: Long) :Future[Double]={
    db.run(balances.filter(_.from === userid).map(_.amount).sum.result).map(_.getOrElse(0.0))
  }
  def findAll(): Future[List[Balance]] = {
  db.run(balances.result).map(_.toList)
}
}
