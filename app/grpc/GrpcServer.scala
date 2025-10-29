package app.grpc

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import io.grpc.{Server, ServerBuilder}
import play.api.inject.ApplicationLifecycle
import play.api.Logging
import notification.notification.NotificationServiceGrpc

@Singleton
class GrpcServer @Inject() (
    svc: NotificationServiceImpl,
    lifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext)
    extends Logging {

  // Start gRPC server
  private val server: Server =
    ServerBuilder
      .forPort(9001)
      .addService(NotificationServiceGrpc.bindService(svc, ec))
      .build()
      .start()

  logger.info("[gRPC] NotificationService server started on :9001")

  // Graceful shutdown when Play stops
  lifecycle.addStopHook { () =>
    Future {
      server.shutdown()
      if (!server.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
        server.shutdownNow()
      }
      logger.info("[gRPC] NotificationService server stopped")
    }
  }
}
