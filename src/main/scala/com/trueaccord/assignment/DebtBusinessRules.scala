package com.trueaccord.assignment

import com.trueaccord.assignment.PaymentsFacade.{Debt, Payment, PaymentPlan}
import org.json4s.{DefaultFormats, JObject, JValue}
import org.json4s.JsonDSL.WithBigDecimal._

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, Period}
import scala.concurrent.Await
import scala.math.Ordered.orderingToOrdered

/**
 * Debt business rules and calculations
 * BigDecimal arithmetic used to achieve the precision
 */
object DebtBusinessRules {

  val dtFormatter = DateTimeFormatter.ISO_DATE

  /**
   * utility function to parse date in ISO format
   * @param strDate
   * @return
   */
  def parseDate(strDate: String) = LocalDate.parse(strDate, dtFormatter)

  /**
   * Utility function to parse/convert payment frequency into number of days
   * @param strPeriod
   * @return
   */
  def parsePeriod(strPeriod: String) =
    strPeriod match {
      case "WEEKLY" => Period.ofDays(7)
      case "BI_WEEKLY" => Period.ofDays(14)
      case _ => throw new IllegalArgumentException(s"Invalid argument, cannot convert: ${strPeriod} ")
    }

  /**
   * Calculates money left to paid into the plan
   * @param plan
   * @param payments
   * @return
   */
  def getRemainingOnPlan(plan: PaymentPlan, payments: List[Payment]) =
    plan.amount_to_pay - payments.foldLeft(BigDecimal(0)) { (acc, p) => acc + p.amount }

  /**
   * Finds the last payment date from the list of payments
   * @param payments
   * @return
   */
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

  /**
   * Calculates remaining money to be paid to close the debt
   * @param debt
   * @param plan
   * @param payments
   * @return
   */
  def getRemaining(debt: Debt, plan: Option[PaymentPlan], payments: List[Payment]) =
    plan.fold(debt.amount) { p => getRemainingOnPlan(p, payments) }

  /**
   * Indicates if the plan is still active and therefore the debt is in the plan
   * @param plan
   * @param payments
   * @return
   */
  def isPaymentPlanActive(plan: Option[PaymentPlan], payments: List[Payment]) =
    plan.fold(false) { p => getRemainingOnPlan(p, payments) > 0.0 }

  /**
   * Produces the next due date for the Payment plan
   * there are a couple of special cases here:
   * - the plan can be paid off - so there no due date
   * - the plan could have been started after some payment activity???
   *   In that case i take the plan start date as the due date
   * - the payment may happen to be made on a random date,
   *   in that case i take the next due date after the payment
   * @param plan
   * @param payments
   * @return
   */
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
  }
}

/**
 * API to transform/enrich the Debt object
 * Operates on pre-parsed Json to keep
 * all the fields as specified in the take-home project description
 */
object DebtTransformations {
  import PaymentsFacade._
  import DebtBusinessRules._
  implicit val formats: DefaultFormats.type = DefaultFormats

  def enrich(client: PaymentsFacade, json: JValue) = {
    try {
      // extract the case class object for easy calculation
      val debt = json.extract[Debt]
      // call payment Plans API to get paymentPlan for this Debt if any
      val plan = Await.result(client.paymentPlansSvc.getAsList("debt_id", debt.id), PaymentsRestClient.timeout).lastOption
      // call Payments API to get Payments for this Debt if any
      val payments = plan.fold(List.empty[Payment]) { p =>
        Await.result(client.paymentsSvc.getAsList("payment_plan_id", p.id), PaymentsRestClient.timeout)}

      // merge extra fields into the incoming Json
      json.asInstanceOf[JObject] ~~
        ("is_in_payment_plan" -> isPaymentPlanActive(plan, payments)) ~~
        ("remaining_amount" -> getRemaining(debt, plan, payments)) ~~
        getNextDueDate(plan, payments).fold[JObject](Nil) { dueDate => "next_payment_due_date" -> dueDate.format(dtFormatter) }
    }
    catch {
      // unknown exception, we assume we continue processing the stream and add an error field into the message
      case ex: Exception => {
        json.asInstanceOf[JObject] ~~ ("errorMessage" -> ex.getMessage)
      }
    }
  }
}
