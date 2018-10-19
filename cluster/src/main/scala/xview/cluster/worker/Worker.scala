package xview.cluster.worker

import java.nio.file.{Path, Paths}
import java.util.UUID

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props, Timers}
import akka.stream.ActorMaterializer
import xview.cluster.api
import xview.cluster.api._

object Worker {
  def props(master: ActorRef) = Props(new Worker(master))

  def wd: Path = Paths.get("/data")
  def bucket: String = "cluster"
  def path(tile: Int)(implicit ctx: ActorContext): String = s"tile-$tile-${ctx.self.path.name}"
}

class Worker(master: ActorRef) extends Actor with Timers with ActorLogging {
  private implicit val mat = ActorMaterializer()
  private val id = UUID.randomUUID.toString

  master ! RegisterWorker(id)

  def ready: Receive = {
    master ! RequestTasking(id)

    {
      case Task(tile) ⇒
        log.info("working on tile {}", tile)
        ProcessTile.number(tile, Worker.wd, api.S3Path(Worker.bucket, Worker.path(tile)))

        context.become(processing(tile))
    }
  }

  def processing(tile: Int): Receive = {
    case Failure(ex) ⇒
      log.error(ex, "failure processing tile")
      master ! TaskFailed(id, tile)

    case ProcessTile.Complete(_) ⇒
      log.info("completed tile {}", id)
      master ! TaskCompleted(id, tile)
      context.become(ready)

    case m ⇒
      log.warning("unexpected {}", m)
  }

  def receive: Receive = ready
}
