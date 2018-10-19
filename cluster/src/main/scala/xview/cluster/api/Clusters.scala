package xview.cluster.api

import akka.actor.{ActorPath, ActorRef, ActorSystem, Address}
import akka.cluster.Cluster
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

object Clusters extends LazyLogging {
  val clusterSeedsKey = "cluster.seeds"

  def actorSystemFor(name: String): ActorSystem = ActorSystem(name, ConfigFactory.load(s"$name-cluster.conf"))

  def clientFor(name: String)(implicit system: ActorSystem): ActorRef = {
    // placeholder for now, will be pulled out of config
    val port = 2551

    implicit val cluster = Cluster(system)

    // could key off of the cluster name passed by caller to support multiple cluster configs
    val seeds = system.settings.config.getString(clusterSeedsKey).split(",")

    val initialContacts =
      seeds.map(seed ⇒ ActorPath.fromString(s"akka.tcp://$name@$seed:$port/system/receptionist")).toSet

    seeds.foreach { seed ⇒
      logger.info(s"initial contact seed: $seed")
      cluster.join(new Address("akka.tcp", name, seed, port))
    }

    system.actorOf(
      ClusterClient.props(
        ClusterClientSettings(system).withInitialContacts(initialContacts)
      ),
      "client"
    )
  }
}
