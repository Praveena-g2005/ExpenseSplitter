package app.grpc

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import app.repositories.NotificationRepository
import app.models.Notification

import notification.notification._ // generated messages
import notification.notification.NotificationServiceGrpc

@Singleton
class NotificationServiceImpl @Inject() (
  repo: NotificationRepository
)(implicit ec: ExecutionContext)
    extends NotificationServiceGrpc.NotificationService {

  override def notify(request: NotifyRequest): Future[NotifyResponse] = {
    val rec = Notification(None, request.expenseId, request.to, request.message)
    repo.create(rec).map { _ =>
      println(s"[gRPC] Notified ${request.to}: ${request.message}")
      NotifyResponse(ok = true, details = "saved")
    }
  }

  override def notifyMany(
    request: NotifyManyRequest
  ): Future[NotifyResponse] = {
    val saves = request.recipients.map { to =>
      notify(NotifyRequest(request.expenseId, to, request.message))
    }
    Future
      .sequence(saves)
      .map(_ => NotifyResponse(ok = true, details = "all saved"))
  }
}
