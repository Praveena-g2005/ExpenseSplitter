package app.grpc

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import io.grpc.ManagedChannelBuilder

import notification.notification._
import notification.notification.NotificationServiceGrpc

@Singleton
class NotificationClient @Inject() (implicit ec: ExecutionContext) {

  private val channel =
    ManagedChannelBuilder.forAddress("localhost", 9001).usePlaintext().build()

  private val stub = NotificationServiceGrpc.stub(channel)

  def notifyMany(expenseId: Long, recipients: Seq[String], message: String): Future[NotifyResponse] = {
    val req = NotifyManyRequest(expenseId.toString, recipients, message)
    stub.notifyMany(req)
  }

  def notifyOne(expenseId: Long, to: String, message: String): Future[NotifyResponse] = {
    val req = NotifyRequest(expenseId.toString, to, message)
    stub.notify(req)
  }
}
