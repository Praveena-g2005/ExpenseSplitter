package app.services

import play.api.libs.json._
import app.repositories.UserRepository
import app.models.{User}
import javax.inject.{Inject,Singleton}
import scala.concurrent.{ExecutionContext,Future}
import app.utils.{ValidationFailure,ValidationResult,ValidationSuccess,Validators}
import play.api.Logging

@Singleton
class UserService @Inject()(
    userRepository: UserRepository
)(implicit ec :ExecutionContext) extends Logging{
    def createUser(name :String, email : String): Future[Either[String , User]] = {
        logger.info(s"Creating User with $email")
        Validators.validateName(name) match {
            case ValidationSuccess => 
                Validators.validateEmail(email) match{
                    case ValidationSuccess =>
                        val sanitizedemail = Validators.sanitizedEmail(email)
                        userRepository.isEmailexists(sanitizedemail).flatMap{exists =>
                            if(exists){
                                logger.info(s"Email Already Exists")
                                Future.successful(Left(s"Email Already Exists :$sanitizedemail"))
                            } else{
                                val user=User(
                                    id = None,
                                    name = Validators.sanitizedName(name),
                                    email = sanitizedemail
                                )
                                userRepository.create(user).map{createduser=>
                                    logger.info(s"user created with User id : ${createduser.id}")
                                    Right(createduser)
                                }.recover{
                                    case ex: Exception =>{
                                        logger.error("User not created , try again later")
                                        Left("User not created ,try agian later")
                                    }
                                }  
                            }
                        }
                    case ValidationFailure(message) =>
                        logger.error(s"Email $email is not valide")
                        Future.successful(Left(message))
                }
            case ValidationFailure(message) =>
                logger.error(s"Invalide name : $name")
                Future.successful(Left(message))
        }
    }

    def getAllUser() :Future[List[User]] ={
        logger.info("Getting all the users")
        userRepository.findAll()
    }

    def getUserById(id :Long): Future[Option[User]] ={
        logger.info(s"User with id :$id")
        userRepository.findById(id)
    }
}