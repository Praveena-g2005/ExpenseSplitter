package app.services

import app.models.User
import app.repositories.UserRepository
import app.utils.PasswordHasher
import app.utils.{ValidationFailure, ValidationResult, ValidationSuccess, Validators}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging

@Singleton
class UserService @Inject() (
  userRepository: UserRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  def createUser(
    name: String,
    email: String,
    password: String
  ): Future[Either[String, User]] = {
    logger.info(s"Creating user with email: $email")

    Validators.validateName(name) match {
      case ValidationSuccess =>
        Validators.validateEmail(email) match {
          case ValidationSuccess =>
            validatePassword(password) match {
              case ValidationSuccess =>
                val sanitizedEmail = Validators.sanitizeEmail(email)
                userRepository.isEmailexists(sanitizedEmail).flatMap { exists =>
                  if (exists) {
                    logger.warn(s"Email already exists: $sanitizedEmail")
                    Future.successful(
                      Left(s"Email $sanitizedEmail is already registered")
                    )
                  } else {
                    val passwordHash = PasswordHasher.hash(password)
                    val user = User(
                      id = None,
                      name = Validators.sanitizeName(name),
                      email = sanitizedEmail,
                      passwordHash = passwordHash
                    )
                    userRepository
                      .create(user)
                      .map { createdUser =>
                        logger
                          .info(s"User created successfully: ${createdUser.id}")
                        Right(createdUser)
                      }
                      .recover {
                        case ex: Exception =>
                          logger
                            .error(s"Failed to create user: ${ex.getMessage}", ex)
                          Left("Failed to create user. Please try again.")
                      }
                  }
                }
              case ValidationFailure(message) =>
                Future.successful(Left(message))
            }
          case ValidationFailure(message) =>
            logger.warn(s"Email validation failed: $message")
            Future.successful(Left(message))
        }
      case ValidationFailure(message) =>
        logger.warn(s"Name validation failed: $message")
        Future.successful(Left(message))
    }
  }

  private def validatePassword(password: String): ValidationResult =
    if (password.length < 8) {
      ValidationFailure("Password must be at least 8 characters")
    } else if (!password.exists(_.isUpper)) {
      ValidationFailure("Password must contain at least one uppercase letter")
    } else if (!password.exists(_.isDigit)) {
      ValidationFailure("Password must contain at least one number")
    } else {
      ValidationSuccess
    }

  def getAllUser(): Future[List[User]] = {
    logger.info("Getting all users")
    userRepository.findAll()
  }

  def getUserById(id: Long): Future[Option[User]] = {
    logger.info(s"User with id: $id")
    userRepository.findById(id)
  }
}
