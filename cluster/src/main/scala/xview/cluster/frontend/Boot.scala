package xview.cluster.frontend

import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import xview.cluster.api.{Clusters, Job, _}
import xview.cluster.node.{Master, Worker}

object Boot extends App with LazyLogging {
  implicit val system = Clusters.actorSystemFor(ClusterName)
  implicit val materializer = ActorMaterializer()
  logger.info("frontend node active")

  val job = Job(100 to 200, List(18, 73))

  val master = system.actorOf(Master.props(job), "master")
  val workers = (1 to 5).map(id ⇒ system.actorOf(Worker.props(master), s"worker-$id"))
}
