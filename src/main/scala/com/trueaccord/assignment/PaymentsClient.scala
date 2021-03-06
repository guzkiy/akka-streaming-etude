package com.trueaccord.assignment

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.{JsonFraming, Sink}
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, native}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Trait to help mocking the API call in testing
 * @tparam T
 */
trait PaymentsClient[T] {
  def runWith[U](sink: Sink[T, Future[U]], prms: Any*): Future[U]
  def listCollectorSink = Sink.fold[List[T], T](List.empty[T]){ (acc, a) => a :: acc }
  def getAsList(prms: Any*) = {
    this.runWith( listCollectorSink , prms:_* )
  }
}

/**
 * Streaming REST client implementation based on Akka-Streams
 *
 * @param serviceCall
 * @param manifest$T$0
 * @param actorSystem
 * @param dispatcher
 * @tparam T
 */
class PaymentsRestClient[T: Manifest](serviceCall: String)
                                     (implicit actorSystem: ActorSystem, dispatcher: ExecutionContextExecutor) extends PaymentsClient[T] {
  implicit val serialization: Serialization.type = native.Serialization
  implicit val formats: DefaultFormats.type = DefaultFormats

  /**
   * the main method to run a Sink implementation on the web-request
   * Sink implementation as well as value types are up to the caller
   * REST parameters can be specified in the list, they are translated
   * into the list key=value pairs and appended to the request
   * @param sink
   * @param prms
   * @tparam U
   * @return
   */
  override def runWith[U](sink: Sink[T, Future[U]], prms: Any*): Future[U] = {
    val callParams = if (prms.isEmpty) "" else "?" + prms.grouped(2).map {
      _.mkString("=")
    }.mkString("&")
    Http().singleRequest(HttpRequest(uri = PaymentsRestClient.baseUrl + serviceCall + callParams))
      .flatMap {
        case r@HttpResponse(status, _, entity, _) =>
          status match {
            case StatusCodes.OK =>
              entity.dataBytes
                .via(JsonFraming.objectScanner(1024))
                .map { data => data.decodeString(Unmarshaller.bestUnmarshallingCharsetFor(entity).nioCharset.name) }
                .map { s: String => serialization.read[T](s) }
                .runWith(sink)
            case _ => Future.failed(
              throw new Exception(s"Server responded with the code ${status.intValue()} and message ${entity}"))
          }
      }
  }
}

/**
 * Some defaults for the REST client
 */
object PaymentsRestClient {
  // service base url, to be read from the configuration
  val baseUrl = "https://my-json-server.typicode.com/druska/trueaccord-mock-payments-api/"
  // default service call timeout, to be read from the configuration
  val timeout: FiniteDuration = 20.seconds
}
