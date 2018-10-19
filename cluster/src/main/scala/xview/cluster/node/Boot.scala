package xview.cluster.node

import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import xview.cluster.api.{Clusters, _}

object Boot extends App with LazyLogging {
  implicit val system = Clusters.actorSystemFor(ClusterName)
  implicit val materializer = ActorMaterializer()

  logger.info("cluster node active")
}
