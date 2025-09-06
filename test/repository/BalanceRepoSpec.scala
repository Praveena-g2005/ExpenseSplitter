package app.repositories

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.db.slick.DatabaseConfigProvider
import app.models.{Balance, BalanceTable}
import scala.concurrent.Future
import slick.jdbc.H2Profile.api._
import scala.concurrent.ExecutionContext
import app.grpc.Module
class BalanceRepositorySpec extends AsyncWordSpec with Matchers with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  // Build a minimal Play app with H2 config and disable gRPC
  val app = new GuiceApplicationBuilder()
    .disable[Module] // disable gRPC to avoid binding errors
    .configure(
      "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
      "slick.dbs.default.db.driver" -> "org.h2.Driver",
      "slick.dbs.default.db.url" -> "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      "slick.dbs.default.db.user" -> "sa",
      "slick.dbs.default.db.password" -> "",
      "grpc.server.enabled" -> false
    ).build()

  val dbConfigProvider = app.injector.instanceOf[DatabaseConfigProvider]
  val repo = new BalanceRepository(dbConfigProvider) {
  override def clear(): Future[Int] =
    db.run(sqlu"DELETE FROM balances") // raw SQL works for H2
}


  // Setup schema before running tests
  private val setup = DBIO.seq(
    TableQuery[BalanceTable].schema.create
  )
  dbConfigProvider.get.db.run(setup).futureValue

  "BalanceRepository" should {

    "save and return all balances" in {
      val balancesToSave = Seq(
        Balance(None, "Alice", "Bob", 50.0),
        Balance(None, "Bob", "Charlie", 30.0)
      )
      for {
        _ <- repo.clear() // clear any existing data
        _ <- repo.saveAll(balancesToSave)
        all <- repo.all()
      } yield {
        all should have size 2
        all.map(_.from) should contain allOf ("Alice", "Bob")
        all.map(_.to) should contain allOf ("Bob", "Charlie")
        all.map(_.amount) should contain allOf (50.0, 30.0)
      }
    }

    "clear all balances" in {
      for {
        _ <- repo.saveAll(Seq(Balance(None, "Alice", "Bob", 50.0)))
        deletedCount <- repo.clear()
        all <- repo.all()
      } yield {
        all shouldBe empty
      }
    }
  }
}
