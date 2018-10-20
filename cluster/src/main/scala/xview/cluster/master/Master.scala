package xview.cluster.master

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import xview.cluster.api._

import scala.collection.immutable

object Master {
  def props(job: Job) = Props(new Master(job))
}

class Master(job: Job) extends Actor with ActorLogging {
  private var tiles = immutable.Queue(job.tiles: _*)
  private var workers = Map[String, ActorRef]()

  // todo;; distribute work evenly across nodes

  def receive: Receive = {
    case RegisterWorker(id) ⇒
      log.info("registered {}", id)
      workers += id → sender

    case RequestTasking(worker) ⇒
      if (tiles.nonEmpty) {
        workers.get(worker) match {
          case Some(ref) ⇒
            val tile = tiles.head
            ref ! Task(tile)
            tiles = tiles.tail

          case None ⇒
            log.warning("worker {} is not registered", worker)
        }
      }

    case TaskCompleted(worker, tile) ⇒
      log.info("worker {} finished processing tile {}", worker, tile)

    case TaskFailed(worker, tile) ⇒
      log.info("worker {} failed to process tile {}", worker, tile)
  }
}
