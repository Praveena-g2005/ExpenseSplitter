package app.repositories

import app.models.{RefreshToken, RefreshTokenTable}
import slick.jdbc.MySQLProfile.api._
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.{ExecutionContext, Future}
import java.sql.Timestamp
import java.time.LocalDateTime

@Singleton
class RefreshTokenRepository @Inject() (
    dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val refreshTokens = TableQuery[RefreshTokenTable]
  private val db = dbConfigProvider.get.db

  def create(refreshToken: RefreshToken): Future[RefreshToken] = {
    val insertQuery =
      refreshTokens returning refreshTokens.map(_.id) += refreshToken
    db.run(insertQuery).map { generatedId =>
      refreshToken.copy(id = Some(generatedId))
    }
  }

  def findByToken(token: String): Future[Option[RefreshToken]] = {
    db.run(refreshTokens.filter(_.token === token).result.headOption)
  }

  def revokeToken(token: String): Future[Int] = {
    val query =
      refreshTokens.filter(_.token === token).map(_.revoked).update(true)
    db.run(query)
  }

  def revokeUserTokens(userId: Long): Future[Int] = {
    val query =
      refreshTokens.filter(_.userId === userId).map(_.revoked).update(true)
    db.run(query)
  }

  def deleteExpiredTokens(): Future[Int] = {
    val now = Timestamp.valueOf(LocalDateTime.now())
    val query = refreshTokens.filter(_.expiresAt < now).delete
    db.run(query)
  }

  def isValid(token: String): Future[Boolean] = {
    val now = Timestamp.valueOf(LocalDateTime.now())
    val query = refreshTokens
      .filter(_.token === token)
      .filter(_.revoked === false)
      .filter(_.expiresAt > now)
      .exists
    db.run(query.result)
  }
}
