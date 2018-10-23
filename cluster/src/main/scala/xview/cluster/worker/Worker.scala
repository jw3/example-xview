package xview.cluster.worker

import java.nio.file.{Path, Paths}

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props, Timers}
import akka.stream.ActorMaterializer
import xview.cluster.api
import xview.cluster.api._

object Worker {
  def props(id: String, master: ActorRef, jobId: String) = Props(new Worker(id, master, jobId))

  def wd: Path = Paths.get("/data")
  def bucket: String = "cluster"
  def path(tile: Int)(implicit ctx: ActorContext): String = s"tile-$tile-${ctx.self.path.name}"
}

class Worker(id: String, master: ActorRef, jobId: String) extends Actor with Timers with ActorLogging {
  private implicit val mat = ActorMaterializer()
  import context.system

  log.info("registering")
  master ! RegisterWorker(id)
  // todo;; should get an ack

  def ready: Receive = {
    master ! RequestTasking(id)

    {
      case Task(tile, filter) ⇒
        log.info("working on tile {}", tile)
        ProcessTile.number(tile,
                           Worker.wd,
                           api.S3Path(Worker.bucket, Worker.path(tile)),
                           filter.getOrElse(Seq.empty),
                           self)

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
