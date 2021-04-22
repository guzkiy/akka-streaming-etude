package com.trueaccord.assignment

import akka.actor.ActorSystem
import com.trueaccord.assignment.PaymentsFacade.{Payment, PaymentPlan}
import org.json4s.{DefaultFormats, JValue}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait  PaymentsFacade {
  def debtsSvc: PaymentsClient[JValue]
  def paymentPlansSvc: PaymentsClient[PaymentPlan]
  def paymentsSvc: PaymentsClient[Payment]
}

object PaymentsFacade {
  case class Debt(id: Int, amount: BigDecimal)
  case class PaymentPlan(id: Int, debt_id: Int, amount_to_pay: BigDecimal, installment_amount: BigDecimal, installment_frequency: String, start_date: String)
  case class Payment(date: String, payment_plan_id: Int, amount: BigDecimal)
}

class PaymentsFacadeRest(implicit val actorSystem: ActorSystem) extends PaymentsFacade {
  implicit val dispatcher: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val formats: DefaultFormats.type = DefaultFormats
  // debts service client
  override def debtsSvc = new PaymentsRestClient[JValue]("debts")
  // paymentPlans service client
  override def paymentPlansSvc = new PaymentsRestClient[PaymentPlan]("payment_plans")
  // payments service client
  override def paymentsSvc = new PaymentsRestClient[Payment]("payments")
}
