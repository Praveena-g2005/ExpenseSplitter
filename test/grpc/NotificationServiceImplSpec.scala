package app.grpc

import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.scalatest.AsyncIdiomaticMockito
import scala.concurrent.Future
import app.repositories.NotificationRepository
import app.models.Notification
import notification.notification._

class NotificationServiceImplSpec extends AsyncWordSpec with Matchers with AsyncIdiomaticMockito {

  implicit val ec = scala.concurrent.ExecutionContext.global

  "NotificationServiceImpl" should {

    "return ok response when notify is called" in {
      // Mock the repository
      val repoMock = mock[NotificationRepository]

      // Stub the repository create method
      repoMock.create(*).returns(Future.successful(1L))

      val service = new NotificationServiceImpl(repoMock)

      val request = NotifyRequest(expenseId = "exp123", to = "user@example.com", message = "Test message")

      service.notify(request).map { response =>
        response.ok shouldBe true
        response.details shouldBe "saved"
        repoMock.create(*) wasCalled once
      }
    }

    "return ok response when notifyMany is called" in {
      val repoMock = mock[NotificationRepository]

      repoMock.create(*).returns(Future.successful(1L))

      val service = new NotificationServiceImpl(repoMock)

      val request = NotifyManyRequest(
        expenseId = "exp456",
        recipients = Seq("user1@example.com", "user2@example.com"),
        message = "Group message"
      )

      service.notifyMany(request).map { response =>
        response.ok shouldBe true
        response.details shouldBe "all saved"
        repoMock.create(*) wasCalled twice
      }
    }
  }
}
