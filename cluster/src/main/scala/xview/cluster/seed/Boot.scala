package xview.cluster.seed

import akka.actor.Address
import akka.cluster.Cluster
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import xview.cluster.api._

object Boot extends App with LazyLogging {
  implicit val system = Clusters.actorSystemFor(ClusterName)
  implicit val materializer = ActorMaterializer()

  val cluster = Cluster(system)

  system.settings.config.getString("cluster.seeds").split(",").foreach { seed ⇒
    println(s"joining seed $seed")
    cluster.join(
      new Address("akka.tcp", ClusterName, seed, 2551)
    )
  }

  logger.info("cluster seed node active")
}
