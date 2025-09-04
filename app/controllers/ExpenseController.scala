package app.controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import app.models.{Expense, Balance}
import app.services.ExpenseService
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ExpenseController @Inject()(cc: ControllerComponents, expenseService: ExpenseService)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  /** POST /expenses */
def createExpense: Action[JsValue] = Action.async(parse.json) { request =>
  request.body.validate[Expense].map { expense =>
    expenseService.addExpense(expense).map { created =>
      Ok(Json.toJson(created))
    }
  }.getOrElse {
    Future.successful(BadRequest(Json.obj("error" -> "Invalid Expense JSON")))
  }
}

def getBalances: Action[AnyContent] = Action.async {
  expenseService.calculateBalances().map { balances =>
    Ok(Json.toJson(balances))
  }
}
  }