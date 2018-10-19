package xview.cluster.worker

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import xview.cluster.api.{FinishedWork, RegisterWorker, StartWork}
import xview.cluster.worker.Worker.TileComplete

import scala.concurrent.duration.DurationInt
import scala.util.Random

object Worker {
  def props(master: ActorRef) = Props(new Worker(master))

  case class TileComplete(id: Int)
}

class Worker(master: ActorRef) extends Actor with Timers with ActorLogging {
  master ! RegisterWorker

  def receive: Receive = {
    case StartWork(tile) ⇒
      log.info("working on tile {}", tile)

      val duration = Random.nextInt(10).seconds
      timers.startSingleTimer("done", Worker.TileComplete(tile), duration)

    case TileComplete(id) ⇒
      log.info("completed tile {}", id)
      master ! FinishedWork(id)
  }
}
