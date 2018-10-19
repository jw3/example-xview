package xview.cluster.worker

import java.nio.file.{Path, Paths}

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props, Timers}
import akka.stream.ActorMaterializer
import xview.cluster.api
import xview.cluster.api.{RegisterWorker, WorkCompleted, WorkFailed, WorkStarted}

object Worker {
  def props(master: ActorRef) = Props(new Worker(master))

  def wd: Path = Paths.get("/data")
  def bucket: String = "cluster"
  def path(tile: Int)(implicit ctx: ActorContext): String = s"tile-$tile-${ctx.self.path.name}"
}

class Worker(master: ActorRef) extends Actor with Timers with ActorLogging {
  implicit val mat = ActorMaterializer()

  master ! RegisterWorker

  def ready: Receive = {
    case WorkStarted(tile) ⇒
      log.info("working on tile {}", tile)
      ProcessTile.number(tile, Worker.wd, api.S3Path(Worker.bucket, Worker.path(tile)))

      context.become(processing(tile))
  }

  def processing(tile: Int): Receive = {
    case Failure(ex) ⇒
      log.error(ex, "failure processing tile")
      master ! WorkFailed(tile)

    case ProcessTile.Complete(id) ⇒
      log.info("completed tile {}", id)
      master ! WorkCompleted(id)

    case m ⇒
      log.warning("unexpected {}", m)
  }

  def receive: Receive = ready
}
