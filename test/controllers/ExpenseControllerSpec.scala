package app.controllers

import org.apache.pekko.stream.Materializer

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
// import akka.stream.Materializer
import play.api.mvc.ControllerComponents
import play.api.inject.guice.GuiceApplicationBuilder
import app.models.{Expense, Balance}
import app.services.ExpenseService
import play.api.test.Helpers.POST

class ExpenseControllerSpec extends PlaySpec with MockitoSugar {

  // Mock the ExpenseService
  val mockExpenseService = mock[ExpenseService]

  // Minimal Play app to get ControllerComponents
  val app = new GuiceApplicationBuilder().build()
  val controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  // Controller instance
  val controller = new ExpenseController(controllerComponents, mockExpenseService)(app.actorSystem.dispatcher)

  // Implicits for Actions
  implicit val mat: Materializer = app.materializer
  implicit val ec: ExecutionContext = app.actorSystem.dispatcher

  "ExpenseController#createExpense" should {

    "return 200 OK with created expense when valid JSON is posted" in {
      val expenseJson = Json.obj(
        "paidBy" -> "Alice",
        "amount" -> 500,
        "participants" -> Json.arr("Alice", "Bob", "Chris")
      )
      val createdExpense = Expense(Some(1), "Alice", 500.0, List("Alice", "Bob", "Chris"))

      // Stub the mock service
      when(mockExpenseService.addExpense(any[Expense]))
        .thenReturn(Future.successful(createdExpense))

      val request = FakeRequest(POST, "/expenses", FakeHeaders(Seq("Content-Type" -> "application/json")), expenseJson)



      val result = controller.createExpense.apply(request)
      println("Response status: " + status(result))
println("Response body: " + contentAsString(result))
println("Response contentType: " + contentType(result))

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val json = contentAsJson(result)
      (json \ "id").as[Long] mustBe 1
      (json \ "paidBy").as[String] mustBe "Alice"

      verify(mockExpenseService).addExpense(any[Expense])
    }

    "return 400 BadRequest when invalid JSON is posted" in {
      val invalidJson = Json.obj("invalidField" -> "value")

    //   val request = FakeRequest(POST, "/expenses")
    //     .withHeaders("Content-Type" -> "application/json")
    //     .withJsonBody(invalidJson)
    val request = FakeRequest(
        POST,
        "/expenses",
        FakeHeaders(Seq("Content-Type" -> "application/json")),
        invalidJson
      )
      val result = controller.createExpense.apply(request)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("application/json")
    }
  }

  "ExpenseController#getBalances" should {

    "return 200 OK with balances JSON" in {
      val balances = Seq(Balance(Some(1), "Bob", "Alice", 50.0))

      // Stub the mock service
      when(mockExpenseService.calculateBalances())
        .thenReturn(Future.successful(balances))

      val request = FakeRequest(GET, "/balances")

      val result = controller.getBalances.apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val json = contentAsJson(result)
      (json(0) \ "from").as[String] mustBe "Bob"
      (json(0) \ "to").as[String] mustBe "Alice"
      (json(0) \ "amount").as[Double] mustBe 50.0

      verify(mockExpenseService).calculateBalances()
    }
  }
}
