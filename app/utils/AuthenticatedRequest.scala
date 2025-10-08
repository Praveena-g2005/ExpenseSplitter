package app.utils

import play.api.mvc.{Request, WrappedRequest}
import app.models.User

case class AuthenticatedRequest[A](user: User, request: Request[A]) extends WrappedRequest[A](request)
