package pilotlight

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class HueBridge private(bridgeIP: String, userToken: String)(implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext) {
  import HueBridge._

  private val baseURL = "http://" + bridgeIP + "/api/" + userToken + "/"

  def lights: Map[String, Light] = {
    implicit val codec: JsonValueCodec[Map[String, Light]] = JsonCodecMaker.make[Map[String, Light]](CodecMakerConfig())
    readFromArray(HueBridge.toStrictString(Http().singleRequest(HttpRequest(uri = baseURL + "lights"))).getBytes())
  }

  def update(llist: Map[String, LightStateUpdate]): Map[String, String] = {
    implicit val codec: JsonValueCodec[LightStateUpdate] = JsonCodecMaker.make[LightStateUpdate](CodecMakerConfig())
    llist map { case l: (String, LightStateUpdate) =>
      val res = HueBridge.toStrictString(Http().singleRequest(HttpRequest(HttpMethods.PUT, uri = baseURL + s"lights/${l._1}/state", entity = writeToArray(l._2))))
      (l._1, res)
    }
  }

  def updateAsync(llist: Map[String, LightStateUpdate]): Map[String, Future[String]] = {
    implicit val codec: JsonValueCodec[LightStateUpdate] = JsonCodecMaker.make[LightStateUpdate](CodecMakerConfig())
    llist map { case l: (String, LightStateUpdate) =>
      val res = HueBridge.toStrictStringAsync(Http().singleRequest(HttpRequest(HttpMethods.PUT, uri = baseURL + s"lights/${l._1}/state", entity = writeToArray(l._2))))
      (l._1, res)
    }
  }

  def close(): Unit = system.terminate()
}

object HueBridge {

  implicit val timeout: FiniteDuration = 10.seconds

  def apply(config: Config): HueBridge = HueBridge(config.getString("hue.ip"), config.getString("hue.user"))
  def apply(bridgeIP: String, userToken: String) = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher
    new HueBridge(bridgeIP, userToken)
  }

  def registerUser(bridgeIP: String, username: String): (String, HueBridge) = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val codec: JsonValueCodec[Array[UserResgisterResponse]] = JsonCodecMaker.make[Array[UserResgisterResponse]](CodecMakerConfig())
    val result = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"http://$bridgeIP/api", entity = s"""{"devicetype":"$username"}"""))
    val userToken = readFromArray(toStrictString(result).getBytes()).apply(0).success.username
    (userToken, new HueBridge(bridgeIP, userToken)(system, mat, ec))
  }

  private def toStrictStringAsync(resp: Future[HttpResponse])(implicit mat: ActorMaterializer, ec: ExecutionContext, timeout: FiniteDuration): Future[String] = {
    for {
      httpResponse <- resp
      entity       <- httpResponse.entity.toStrict(timeout)
    } yield {
      entity match {
        case HttpEntity.Strict(_, data) => data.utf8String
      }
    }
  }

  private def toStrictString(resp: Future[HttpResponse])(implicit mat: ActorMaterializer, ec: ExecutionContext, timeout: FiniteDuration): String = {
    Await.result(toStrictStringAsync(resp), timeout)
  }
}

final case class User(username: String)
final case class UserResgisterResponse(success: User)

final case class Light(
  @named("type") model: String,
  state: LightState,
  name: String,
  modelid: String,
  manufacturername: String,
  uniqueid: String,
  swversion: String,
)

final case class LightState(
  on: Boolean,
  bri: Int,
  hue: Option[Int],
  sat: Option[Int],
  xy: Array[Float],
  ct: Option[Int],
  alert: String,
  effect: Option[String],
  colormode: String,
  reachable: Boolean,
)

final case class LightStateUpdate(
  on: Option[Boolean] = None,
  bri: Option[Int] = None,
  hue: Option[Int] = None,
  sat: Option[Int] = None,
  xy: Option[Array[Float]] = None,
  ct: Option[Int] = None,
  alert: Option[String] = None,
  effect: Option[String] = None,
  transitiontime: Option[Int] = None,
  bri_inc: Option[Int] = None,
  hue_inc: Option[Int] = None,
  sat_inc: Option[Int] = None,
  xy_inc: Option[Array[Float]] = None,
  ct_inc: Option[Int] = None,
)
