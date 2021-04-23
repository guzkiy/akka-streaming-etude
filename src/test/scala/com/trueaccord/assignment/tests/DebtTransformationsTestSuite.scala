package com.trueaccord.assignment.tests

import com.trueaccord.assignment.PaymentsFacade.{Debt, Payment, PaymentPlan}
import com.trueaccord.assignment.{DebtTransformations, PaymentsClient, PaymentsFacade}
import org.json4s.{DefaultFormats, JString, JValue, native}
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.scalatest.funsuite.AnyFunSuite
import org.scalamock.scalatest.MockFactory
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DebtTransformationsTestSuite  extends AnyFunSuite with MockFactory {
  implicit val formats: DefaultFormats.type = DefaultFormats
  implicit val serialization: Serialization.type = native.Serialization
  val debtWithPlan =
    (Debt(2,4920.34),
     Some(PaymentPlan(2,2,4920.34,1230.085,"BI_WEEKLY","2020-01-01")),
     List(Payment("2020-08-08",2,4312.67)) )

  def mockPaymentsFacade(debt: Debt, planMayBe: Option[PaymentPlan], payments:List[Payment]) = {
    val paymentPlansSvcMock = mock[PaymentsClient[PaymentPlan]]
    (paymentPlansSvcMock.getAsList _).expects(*).returns( Future(planMayBe.fold(List.empty[PaymentPlan])(List(_))) )
    val paymentsSvcMock = mock[PaymentsClient[Payment]]
    (paymentsSvcMock.getAsList _).expects(*).returns( Future(payments) )
    new PaymentsFacade {
      override def debtsSvc: PaymentsClient[JValue] = throw new NotImplementedException
      override def paymentPlansSvc: PaymentsClient[PaymentPlan] = paymentPlansSvcMock
      override def paymentsSvc: PaymentsClient[Payment] = paymentsSvcMock
    }
  }

  test("enrich Debt with a payment plan") {
    val debt = debtWithPlan._1
    val paymentsFacade = Function.tupled(mockPaymentsFacade _)(debtWithPlan)
    val debtJson = parse(serialization.write(debt), useBigDecimalForDouble = true)
    val debtEnriched = DebtTransformations.enrich(paymentsFacade, debtJson)
    assert((debtEnriched  \ "id").values == debt.id)
    assert((debtEnriched  \ "is_in_payment_plan").values == true)
    assert((debtEnriched  \ "remaining_amount").values == BigDecimal("607.67"))
    // make sure ISO format
    assert((debtEnriched \ "next_payment_due_date") == JString("2020-08-12"))
  }
}