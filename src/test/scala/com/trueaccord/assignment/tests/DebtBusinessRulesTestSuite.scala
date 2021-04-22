package com.trueaccord.assignment.tests
import com.trueaccord.assignment.DebtBusinessRules
import com.trueaccord.assignment.PaymentsFacade.{Debt, Payment, PaymentPlan}
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class DebtBusinessRulesTestSuite extends AnyFunSuite {
  val debtWithPlan =
    (Debt(2,4920.34),
      Some(PaymentPlan(2,2,1920.34,1230.085,"BI_WEEKLY","2020-01-01")),
      List(Payment("2020-08-08",2,1230.085)) )

  val debtWithNoPlan =
    (Debt(2,4920.34),
      Option.empty[PaymentPlan],
      List())

  val debtPaidOff =
    (Debt(2,4920.34),
      Some(PaymentPlan(2,2,1000.1,1230.085,"BI_WEEKLY","2020-01-01")),
      List(Payment("2020-01-08",2,500.0), Payment("2020-08-08",2,500.1)  ) )

  val debtWithPaymentBeforePlanStart =
    (Debt(2,4920.34),
      Some(PaymentPlan(2,2,1920.34,1230.085,"BI_WEEKLY","2020-01-01")),
      List(Payment("2019-08-08",2,1230.085)) )

  test("is_in_payment_plan tests")  {
      val (debt, planMayBe, payments) = debtWithPlan
      assert( DebtBusinessRules.isInPaymentPlan(planMayBe, payments))
  }

  test("is_in_payment_plan tests with no plan")  {
      val (debt, planMayBe, payments) = debtWithNoPlan
      assert(! DebtBusinessRules.isInPaymentPlan(planMayBe, payments))
  }

  test("is_in_payment_plan tests debt paid off")  {
      val (debt, planMayBe, payments) = debtPaidOff
      assert(! DebtBusinessRules.isInPaymentPlan(planMayBe, payments))
  }

  test("remaining amount for debts in progress") {
    val (debt, planMayBe, payments) = debtWithPlan
    assert( BigDecimal("690.255") == DebtBusinessRules.getRemainingOnPlan(planMayBe.get, payments))
  }

  test("remaining amount for debts paid off") {
    val (debt, planMayBe, payments) = debtPaidOff
    assert( BigDecimal("0.0") == DebtBusinessRules.getRemainingOnPlan(planMayBe.get, payments))
  }

  test("next payment due") {
    val (debt, planMayBe, payments) = debtWithPlan
    assert( Some(LocalDate.of(2020, 8, 12))  == DebtBusinessRules.getNextDueDate(planMayBe, payments))
  }

  test("next payment due with payments before the plan start date") {
    val (debt, planMayBe, payments) = debtWithPaymentBeforePlanStart
    assert( Some(DebtBusinessRules.parseDate(planMayBe.get.start_date))  == DebtBusinessRules.getNextDueDate(planMayBe, payments))
  }

  test("next payment due with no plan") {
    val (debt, planMayBe, payments) = debtWithNoPlan
    assert( Option.empty[LocalDate] == DebtBusinessRules.getNextDueDate(planMayBe, payments))
  }

  test("next payment due with paid off debt") {
    val (debt, planMayBe, payments) = debtPaidOff
    assert( Option.empty[LocalDate] == DebtBusinessRules.getNextDueDate(planMayBe, payments))
  }
}
