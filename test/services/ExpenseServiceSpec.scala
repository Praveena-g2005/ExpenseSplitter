
package services

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import app.models.{Expense, Balance}
import app.repositories.{ExpenseRepository, BalanceRepository}
import app.grpc.NotificationClient
import app.services.ExpenseService
import notification.notification.NotifyResponse

class ExpenseServiceSpec
  extends PlaySpec
    with MockitoSugar
    with ScalaFutures {

  "ExpenseService.addExpense" should {

    "save an expense and return it with an ID" in {
      val mockExpenseRepo = mock[ExpenseRepository]
      val mockBalanceRepo = mock[BalanceRepository]
      val mockNotificationClient = mock[NotificationClient]

      val service = new ExpenseService(mockExpenseRepo, mockBalanceRepo, mockNotificationClient)

      val expense = Expense(None, "Alice", 100.0, List("Alice", "Bob"))

      when(mockExpenseRepo.create(any[Expense])).thenReturn(Future.successful(1L))
      when(mockNotificationClient.notifyMany(anyLong(), any(), any()))
        .thenReturn(Future.successful(NotifyResponse(ok = true, details = "sent")))

      val resultF = service.addExpense(expense)

      whenReady(resultF) { result =>
        result.id mustBe Some(1L)
        result.paidBy mustBe "Alice"
        result.amount mustBe 100.0
      }

      verify(mockExpenseRepo).create(any[Expense])
      verify(mockNotificationClient).notifyMany(anyLong(), any(), any())
    }

    "fail gracefully when ExpenseRepository.create fails" in {
      val mockExpenseRepo = mock[ExpenseRepository]
      val mockBalanceRepo = mock[BalanceRepository]
      val mockNotificationClient = mock[NotificationClient]

      val service = new ExpenseService(mockExpenseRepo, mockBalanceRepo, mockNotificationClient)
      val expense = Expense(None, "Alice", 100.0, List("Alice", "Bob"))

      when(mockExpenseRepo.create(any[Expense])).thenReturn(Future.failed(new RuntimeException("DB error")))

      val resultF = service.addExpense(expense)

      whenReady(resultF.failed) { ex =>
        ex.getMessage mustBe "DB error"
      }

      verify(mockExpenseRepo).create(any[Expense])
      verifyNoInteractions(mockNotificationClient)
    }

    "recover when NotificationClient.notifyMany fails" in {
      val mockExpenseRepo = mock[ExpenseRepository]
      val mockBalanceRepo = mock[BalanceRepository]
      val mockNotificationClient = mock[NotificationClient]

      val service = new ExpenseService(mockExpenseRepo, mockBalanceRepo, mockNotificationClient)
      val expense = Expense(None, "Alice", 100.0, List("Alice", "Bob"))

      when(mockExpenseRepo.create(any[Expense])).thenReturn(Future.successful(1L))
      when(mockNotificationClient.notifyMany(anyLong(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException("gRPC failed")))

      val resultF = service.addExpense(expense)

      whenReady(resultF) { result =>
        result.id mustBe Some(1L)
      }

      verify(mockExpenseRepo).create(any[Expense])
      verify(mockNotificationClient).notifyMany(anyLong(), any(), any())
    }
  }

  "ExpenseService.calculateBalances" should {

    "calculate debts correctly" in {
      val mockExpenseRepo = mock[ExpenseRepository]
      val mockBalanceRepo = mock[BalanceRepository]
      val mockNotificationClient = mock[NotificationClient]

      val service = new ExpenseService(mockExpenseRepo, mockBalanceRepo, mockNotificationClient)

      val expenses = Seq(
        Expense(Some(1), "Alice", 100, List("Alice", "Bob")),  // Bob owes Alice 50
        Expense(Some(2), "Bob", 60, List("Alice", "Bob"))      // Alice owes Bob 30
      )

      when(mockExpenseRepo.all()).thenReturn(Future.successful(expenses))
      when(mockBalanceRepo.clear()).thenReturn(Future.successful(1))
      when(mockBalanceRepo.saveAll(any[Seq[Balance]])).thenReturn(Future.successful(()))

      val resultF = service.calculateBalances()

      whenReady(resultF) { balances =>
        balances.size mustBe 1
        balances.head.from mustBe "Bob"
        balances.head.to mustBe "Alice"
        balances.head.amount mustBe 20.0
      }
    }

    "return empty list if no expenses" in {
      val mockExpenseRepo = mock[ExpenseRepository]
      val mockBalanceRepo = mock[BalanceRepository]
      val mockNotificationClient = mock[NotificationClient]

      val service = new ExpenseService(mockExpenseRepo, mockBalanceRepo, mockNotificationClient)

      when(mockExpenseRepo.all()).thenReturn(Future.successful(Seq.empty))

      val resultF = service.calculateBalances()

      whenReady(resultF) { balances =>
        balances mustBe empty
      }
    }

    "ignore self-payments" in {
      val mockExpenseRepo = mock[ExpenseRepository]
      val mockBalanceRepo = mock[BalanceRepository]
      val mockNotificationClient = mock[NotificationClient]

      val service = new ExpenseService(mockExpenseRepo, mockBalanceRepo, mockNotificationClient)

      val expenses = Seq(Expense(Some(1), "Alice", 100, List("Alice")))
      when(mockExpenseRepo.all()).thenReturn(Future.successful(expenses))
      when(mockBalanceRepo.clear()).thenReturn(Future.successful(1))
      when(mockBalanceRepo.saveAll(any[Seq[Balance]])).thenReturn(Future.successful(()))

      val resultF = service.calculateBalances()

      whenReady(resultF) { balances =>
        balances mustBe empty
      }
    }
  }

  "ExpenseService.addExpenseAndUpdateBalances" should {
    "add expense and recalc balances" in {
      val mockExpenseRepo = mock[ExpenseRepository]
      val mockBalanceRepo = mock[BalanceRepository]
      val mockNotificationClient = mock[NotificationClient]

      val service = new ExpenseService(mockExpenseRepo, mockBalanceRepo, mockNotificationClient)
      val expense = Expense(None, "Alice", 100.0, List("Alice", "Bob"))

      when(mockExpenseRepo.create(any[Expense])).thenReturn(Future.successful(1L))
      when(mockNotificationClient.notifyMany(anyLong(), any(), any()))
        .thenReturn(Future.successful(NotifyResponse(ok = true, details = "sent")))

      val expenses = Seq(expense.copy(id = Some(1)))
      val balances = Seq(Balance(None, "Bob", "Alice", 50))

      when(mockExpenseRepo.all()).thenReturn(Future.successful(expenses))
      when(mockBalanceRepo.clear()).thenReturn(Future.successful(1))
      when(mockBalanceRepo.saveAll(any[Seq[Balance]])).thenReturn(Future.successful(()))

      val resultF = service.addExpenseAndUpdateBalances(expense)

      whenReady(resultF) { result =>
        result mustBe balances
      }
    }
  }
}
