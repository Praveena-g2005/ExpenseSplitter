package app.grpc

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchers
import scala.concurrent.Future
import notification.notification._

class NotificationClientSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  implicit val ec = scala.concurrent.ExecutionContext.global

  "NotificationClient" should {

    "call notifyOne and return a successful response" in {
      val mockStub = mock[NotificationServiceGrpc.NotificationServiceStub]

      val mockResponse = Future.successful(NotifyResponse(ok = true, details = "saved"))

      when(mockStub.notify(ArgumentMatchers.any[NotifyRequest]))
        .thenReturn(mockResponse)

      val client = new NotificationClient {
        private val stub = mockStub
      }

      client.notifyOne(1, "alice@example.com", "Test Message").map { response =>
        response.ok shouldBe true
        response.details shouldBe "saved"
      }
    }

    "call notifyMany and return a successful response" in {
      val mockStub = mock[NotificationServiceGrpc.NotificationServiceStub]

      val mockResponse = Future.successful(NotifyResponse(ok = true, details = "all saved"))

      when(mockStub.notifyMany(ArgumentMatchers.any[NotifyManyRequest]))
        .thenReturn(mockResponse)

      val client = new NotificationClient {
        private val stub = mockStub
      }

      client.notifyMany(1, Seq("alice@example.com", "bob@example.com"), "Test Many").map { response =>
        response.ok shouldBe true
        response.details shouldBe "all saved"
      }
    }
  }
}
