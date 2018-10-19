package xview.cluster.master

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import xview.cluster.api.{FinishedWork, Job, RegisterWorker, StartWork}

import scala.collection.immutable

object Master {
  def props(job: Job) = Props(new Master(job))
}

class Master(job: Job) extends Actor with ActorLogging {
  var tiles = immutable.Queue(job.tiles: _*)
  var workers = Map[String, ActorRef]()

  def receive: Receive = {
    case RegisterWorker ⇒
      val id = sender.path.name
      if (!workers.contains(id)) {
        workers += id → sender

        if (tiles.nonEmpty) {
          val tile = tiles.head
          sender ! StartWork(tile)
          tiles = tiles.tail
        }
      }

    case FinishedWork(_) ⇒
      if (tiles.nonEmpty) {
        val tile = tiles.head
        sender ! StartWork(tile)
        tiles = tiles.tail
      }
  }
}
