package xview.cluster.frontend

import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util
import com.typesafe.scalalogging.LazyLogging
import xview.cluster.api.{Clusters, _}
import xview.cluster.master.ControllerSingleton

import scala.concurrent.duration.DurationInt

object Boot extends App with HttpInterface with LazyLogging {
  implicit val system = Clusters.actorSystemFor(ClusterName)
  implicit val mat = ActorMaterializer()

  logger.info("frontend node active")

  val controllerProxy = system.actorOf(ControllerSingleton.proxyProps(system), name = "controllerProxy")

  implicit val timeout = util.Timeout(10.seconds)
  Http().bindAndHandle(routes(controllerProxy), "0.0.0.0", 9000)
}
