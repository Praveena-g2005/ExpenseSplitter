package app.controllers

import app.services.UserService
import play.api.libs.json._
import javax.inject.{Inject, Singleton}
import app.dtos.{CreateUserRequest, UserResponse, ErrorResponse}
import play.api.Logging
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import app.dtos.CreateUserRequest._ 
@Singleton
class UserController @Inject() (
    userService: UserService,
    cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with Logging {
  def createUser(): Action[JsValue] = Action.async(parse.json) { request =>
    logger.info(s"Initiating for create user")
    request.body.validate[CreateUserRequest] match {
      case JsSuccess(createrequest, _) =>
        userService
          .createUser(createrequest.name, createrequest.email)
          .map {
            case Right(user) =>
              logger.info(s"user created successfully with ${user.id}")
              Created(Json.toJson(UserResponse.fromUser(user)))
            case Left(errormessage) =>
              logger.warn(s"User creation failed : $errormessage")
              BadRequest(Json.toJson(ErrorResponse(errormessage)))
          }
          .recover { case ex: Exception =>
            logger.error(s"unexpected error :${ex.getMessage}")
            InternalServerError(
              Json.toJson(ErrorResponse("Internal Server Error"))
            )
          }
      case JsError(errors) => {
        val errordetails = formatJsonErrors(errors)
        logger.warn(s"Invalid Json Format $errordetails")
        Future.successful(
          BadRequest(
            Json.toJson(
              ErrorResponse(
                error = "Invalid Json Format",
                details = Some(errordetails)
              )
            )
          )
        )
      }
    }
  }

  def getAllUsers() :Action[AnyContent] = Action.async {
    logger.info(s"Get all user Request received")
    userService.getAllUser().map{users =>
        val userresponse = users.map(UserResponse.fromUser)
        Ok(Json.toJson(userresponse))
    }.recover{
        case ex:Exception=>
            logger.error(s"Error in fetching user : ${ex.getMessage}")
            InternalServerError(Json.toJson(ErrorResponse("Failed Fetching users")))
    }
  }

  def getUserById(id :Long) :Action[AnyContent] = Action.async {
    logger.info(s"get user by id request received")
    userService.getUserById(id).map{
        case Some(user)=>
            Ok(Json.toJson(UserResponse.fromUser(user)))
        case None =>
            logger.warn(s"User not found with the id :$id")
            NotFound(Json.toJson(ErrorResponse(s"User not found with id : $id")))
    }.recover{
        case ex:Exception =>
            logger.error(s"Error in fetching user : ${ex.getMessage}")
            InternalServerError(Json.toJson(ErrorResponse(s"Failed Fetching user with id :$id")))
    }
  }

  private def formatJsonErrors(
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]
  ): Map[String, String] = {
    errors.map {
      case (path, validationErrors) =>
        val field = path.toJsonString.replaceAll("^\\.|^/", "")
        val messages = validationErrors.map(_.message).mkString(", ")
        field -> messages
    }.toMap
  }
}
