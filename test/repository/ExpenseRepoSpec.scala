package app.repositories

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.db.slick.DatabaseConfigProvider
import app.models.{Expense, ExpenseTable}
import slick.jdbc.H2Profile.api._
import scala.concurrent.ExecutionContext
import app.grpc._
class ExpenseRepositorySpec extends AsyncWordSpec with Matchers with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  // Build a minimal Play app with H2 config
  val app = new GuiceApplicationBuilder()
  .disable[Module] // disable your gRPC module entirelysbt
    .configure(
      "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
      "slick.dbs.default.db.driver" -> "org.h2.Driver",
      "slick.dbs.default.db.url" -> "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      "slick.dbs.default.db.user" -> "sa",
      "slick.dbs.default.db.password" -> "",
      "grpc.server.enabled" -> false 
    ).build()

  val dbConfigProvider = app.injector.instanceOf[DatabaseConfigProvider]
  val repo = new ExpenseRepository(dbConfigProvider)

  // Setup schema before running tests
  private val setup = DBIO.seq(
    TableQuery[ExpenseTable].schema.create
  )
  dbConfigProvider.get.db.run(setup).futureValue

  "ExpenseRepository" should {

    "create and find an expense" in {
      val expense = Expense(None, "Alice", 500.0, List("Alice", "Bob"))
      for {
        id <- repo.create(expense)
        found <- repo.findById(id)
      } yield {
        found.isDefined shouldBe true
        found.get.paidBy shouldBe "Alice"
        found.get.amount shouldBe 500.0
        found.get.participants should contain allOf ("Alice", "Bob")
      }
    }

    "return all expenses" in {
      val expense = Expense(None, "Bob", 300.0, List("Bob", "Alice"))
      for {
        _ <- repo.create(expense)
        all <- repo.all()
      } yield {
        all should not be empty
        all.map(_.paidBy) should contain("Bob")
      }
    }
  }
}
