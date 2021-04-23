package com.trueaccord.assignment

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.json4s.JValue
import org.json4s.native.JsonMethods.{compact, render}
import scala.concurrent.Await

/**
 * Main class for the tool
 */
object AdminTool {

  def main(args: Array[String]): Unit = {
    val actorSystem: ActorSystem = ActorSystem.create("AdminTool")
    val client = new PaymentsFacadeRest()(actorSystem)
    // sink implementation to enrich and output to the console
    val debtJsonPrintSink = Sink.foreach[JValue]{ s=>println( compact(render(DebtTransformations.enrich(client,s)) )) }
    // debt service call and starting the stream
    val debtsCall = client.debtsSvc.runWith(debtJsonPrintSink)
    // shut down Akka system when done
    debtsCall.onComplete{ _ => actorSystem.terminate() }(actorSystem.dispatcher)
    Await.result(debtsCall, PaymentsRestClient.timeout)
  }
}

