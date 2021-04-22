package com.trueaccord.assignment

import com.trueaccord.assignment.PaymentsFacade.{Debt, Payment, PaymentPlan}
import org.json4s.{DefaultFormats, JObject, JValue}
import org.json4s.JsonDSL.WithBigDecimal._

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, Period}
import scala.concurrent.Await
import scala.math.Ordered.orderingToOrdered

object DebtBusinessRules {

  val dtFormatter = DateTimeFormatter.ISO_DATE

  def parseDate(strDate: String) = LocalDate.parse(strDate, dtFormatter)

  def parsePeriod(strPeriod: String) =
    strPeriod match {
      case "WEEKLY" => Period.ofDays(7)
      case "BI_WEEKLY" => Period.ofDays(14)
      case _ => throw new IllegalArgumentException(s"Invalid argument, cannot convert: ${strPeriod} ")
    }

  def getRemainingOnPlan(plan: PaymentPlan, payments: List[Payment]) =
    plan.amount_to_pay - payments.foldLeft(BigDecimal(0)) { (acc, p) => acc + p.amount }

  def getLastPaymentDate(payments: List[Payment]) = {
    if (payments.nonEmpty) {
      val lastDate = payments.foldLeft(LocalDate.MIN) { (lastDate, i) =>
        val paymentDate = parseDate(i.date)
        if (paymentDate > lastDate) paymentDate else lastDate
      }
      Some(lastDate)
    }
    else None
  }

  def getRemaining(debt: Debt, plan: Option[PaymentPlan], payments: List[Payment]) =
    plan.fold(debt.amount) { p => getRemainingOnPlan(p, payments) }

  def isInPaymentPlan(plan: Option[PaymentPlan], payments: List[Payment]) =
    plan.fold(false) { p => getRemainingOnPlan(p, payments) > 0.0 }

  def getNextDueDate(plan: Option[PaymentPlan], payments: List[Payment]): Option[LocalDate] = {
    plan.fold(Option.empty[LocalDate]) { p =>
      if (!(getRemainingOnPlan(p, payments) > 0.0)) { None }
      else {
        val startDate = parseDate(p.start_date)
        val period = parsePeriod(p.installment_frequency)
        val lastPayMayBe = getLastPaymentDate(payments)
        val nextPayDate = lastPayMayBe.fold(startDate) { lastPayDate =>
          if (lastPayDate > startDate) {
            val numPeriods = ChronoUnit.DAYS.between(startDate, lastPayDate) / period.getDays + 1
            startDate.plusDays(numPeriods * period.getDays)
          }
          else startDate
        }
        Some(nextPayDate)
      }
    }
    // (Debt(2,4920.34),Some(PaymentPlan(2,2,4920.34,1230.085,BI_WEEKLY,2020-01-01)),List(Payment(2020-08-08,2,4312.67)))
  }
}

object DebtTransformations {
  import PaymentsFacade._
  import DebtBusinessRules._
  implicit val formats: DefaultFormats.type = DefaultFormats

  def enrich(json: JValue)(client: PaymentsFacade) = {
    val debt = json.extract[Debt]
    val plan = Await.result(client.paymentPlansSvc.getAsList("debt_id",debt.id), PaymentsRestClient.timeout).lastOption
    val payments = plan.fold( List.empty[Payment] ) { p =>
      Await.result(client.paymentsSvc.getAsList("payment_plan_id", p.id), PaymentsRestClient.timeout)
    }
    println(    (debt, plan, payments))
    json.asInstanceOf[JObject] ~~
      ("is_in_payment_plan" -> isInPaymentPlan(plan, payments)) ~~
      ("remaining_amount" -> getRemaining(debt, plan, payments)) ~~
      getNextDueDate(plan, payments).fold[JObject](Nil) { dueDate => "next_payment_due_date" -> dueDate.format(dtFormatter) }
  }
}
