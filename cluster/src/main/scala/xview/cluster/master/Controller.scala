package xview.cluster.master

import akka.actor.{Actor, ActorLogging, Deploy, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member}
import akka.remote.RemoteScope
import xview.cluster.Roles
import xview.cluster.api.{JobAccepted, SubmitJob, _}
import xview.cluster.worker.Worker

import scala.collection.Map

object Controller {
  def props() = Props(new Controller)
}

//
// todo;; eventually members will contain job context in their roles
//     ;; to allow elasticity and prevent actors bleeding across nodes
//
class Controller extends Actor with ActorLogging {
  val cluster: Cluster = Cluster(context.system)
  var masters: Map[String, Job] = Map.empty
  var backend: Map[String, Member] = Map.empty

  def receive: Receive = {
    case SubmitJob(_, _) if backend.isEmpty ⇒
      sender ! JobRejected

    case SubmitJob(job, workers) ⇒
      sender ! JobAccepted

      val master = cluster.system.actorOf(
        Master.props(job),
        "master"
      )

      var id = 0
      val wpn = workers / backend.size
      val xtra = workers % backend.size
      log.info("job will have [{}]:workers per node:[{}] + [{}]:extra", wpn, backend.size, xtra)

      if (wpn > 0)
        backend.values
          .grouped(wpn)
          .foreach(_.foreach { node ⇒
            cluster.system.actorOf(
              Worker.props(master).withDeploy(Deploy(scope = RemoteScope(node.address))),
              s"worker-$id"
            )
            id += 1
          })

      if (xtra > 0)
        backend.values
          .take(xtra)
          .foreach { node ⇒
            cluster.system.actorOf(
              Worker.props(master).withDeploy(Deploy(scope = RemoteScope(node.address))),
              s"worker-$id"
            )
            id += 1
          }

    case MemberUp(member) ⇒
      if (member.roles.contains(Roles.Worker)) {
        log.info("=====> backend node up: {}", member.address)
        backend += member.address.toString → member
      }

    case UnreachableMember(member) ⇒
      log.info("=====> node unreachable: {}", member)

    case MemberRemoved(member, previousStatus) ⇒
      log.info("=====> node removed: {} after {}", member.address, previousStatus)

    case _: MemberEvent ⇒
  }

  override def preStart(): Unit =
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember]
    )

  override def postStop(): Unit = cluster.unsubscribe(self)
}
