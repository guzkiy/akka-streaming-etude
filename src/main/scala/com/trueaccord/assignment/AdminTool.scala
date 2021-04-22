package com.trueaccord.assignment

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Flow, JsonFraming, Sink}
import com.trueaccord.assignment.PaymentsFacade.{Debt, Payment, PaymentPlan}
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, JObject, JValue, native}
//import org.json4s.JsonDSL._
import org.json4s.JsonDSL.WithBigDecimal._
import org.json4s.native.JsonMethods.{compact, render}

import java.time.{LocalDate, OffsetDateTime, Period}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.math.Ordered.orderingToOrdered



object AdminTool {

  def main(args: Array[String]): Unit = {
    val actorSystem: ActorSystem = ActorSystem.create("AdminTool")
    val client = new PaymentsFacadeRest()(actorSystem)
    val debtJsonPrintSink = Sink.foreach[JValue]{ s=>println( compact(render(DebtTransformations.enrich(s)(client))) ) }
    val debtsCall = client.debtsSvc.runWith(debtJsonPrintSink)
    debtsCall.onComplete{ _ => actorSystem.terminate() }(actorSystem.dispatcher)
    Await.result(debtsCall, PaymentsRestClient.timeout)
  }
}

