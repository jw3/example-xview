package xview.cluster.seed

import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import xview.cluster.api._

object Boot extends App with LazyLogging {
  implicit val system = Clusters.actorSystemFor(ClusterName)
  implicit val materializer = ActorMaterializer()

  logger.info("cluster seed node active")
}

