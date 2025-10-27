package app.repositories

import app.models.{RevokedToken, RevokedTokenTable}
import slick.jdbc.MySQLProfile.api._
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.{ExecutionContext, Future}
import java.sql.Timestamp
import java.time.LocalDateTime

@Singleton
class RevokedTokenRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {
  
  private val revokedTokens = TableQuery[RevokedTokenTable]
  private val db = dbConfigProvider.get.db
  
  def create(revokedToken: RevokedToken): Future[RevokedToken] = {
    val insertQuery = revokedTokens returning revokedTokens.map(_.id) += revokedToken
    db.run(insertQuery).map { generatedId =>
      revokedToken.copy(id = Some(generatedId))
    }.recover {
      case _: Exception =>
        revokedToken
    }
  }

  def isTokenRevoked(token: String): Future[Boolean] = {
    val query = revokedTokens.filter(_.token === token).exists
    db.run(query.result)
  }
  
  def deleteExpiredTokens(): Future[Int] = {
    val now = Timestamp.valueOf(LocalDateTime.now())
    val query = revokedTokens.filter(_.expiresAt < now).delete
    db.run(query)
  }
  
  def revokeToken(token: String, userId: Long, expiresAt: Timestamp ,tokenType :String): Future[RevokedToken] = {
    val now = Timestamp.valueOf(LocalDateTime.now())
    val revokedToken = RevokedToken(
      id = None,
      token = token,
      userId = userId,
      tokenType = tokenType,
      revokedAt = now,
      expiresAt = expiresAt
    )
    create(revokedToken)
  }
}