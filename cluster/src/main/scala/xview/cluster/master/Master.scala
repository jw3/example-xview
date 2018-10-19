package xview.cluster.master

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import xview.cluster.api.{WorkCompleted, Job, RegisterWorker, WorkStarted}

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
          sender ! WorkStarted(tile)
          tiles = tiles.tail
        }
      }

    case WorkCompleted(_) ⇒
      if (tiles.nonEmpty) {
        val tile = tiles.head
        sender ! WorkStarted(tile)
        tiles = tiles.tail
      }
  }
}
