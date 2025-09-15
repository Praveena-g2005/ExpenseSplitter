package app.repositories

import app.models.{User, UserTable}
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Future
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

@Singleton
class UserRepository @Inject()(dbConfigProvider: DatabaseConfigProvider) {
  
  private val users = TableQuery[UserTable]
  private val db = dbConfigProvider.get.db
  
  def findById(id : Long) : Future[Option[User]]{
    db.run(users.filter(_.id===id).result.headOption) 
  }

  def findByEmail(email : String) : Future[Option[User]]{
    db.run(users.filter(_.email===email).result.headOption)
  }

  def create (user: User) :Future[User]{
    val userwithoutId = user.copy(id = None)
    val insertQuery = users returning users.map(_.id) += userwithoutId
    db.run(insertQuery).map { generatedId =>
    user.copy(id = Some(generatedId))
    }
  }

  def updateUser(user : User) : Future[User]{
    val updateQuery = users.filter(_.id===user.id.get).update(user)
    db.run(updateQuery).map{_ =>
        user
    }
  }

  def findAll() :Future[List[User]]{
    db.run(users.result).map(_.toList)
  }

  def findByIds(ids : List[Long]) : Future[List[User]]{
    db.run(users.filter(_.id inSet ids).result).map(_.toList)
  }

  def isEmailexists (email : String) : Future[Boolean]{
    findByEmail(email).map(_.isDefined)
  }
}