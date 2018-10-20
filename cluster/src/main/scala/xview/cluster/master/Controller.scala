package xview.cluster.master

import java.util.UUID

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
    case SubmitJob(_) if backend.isEmpty ⇒
      sender ! JobRejected

    case SubmitJob(desc) ⇒
      val job = Job(UUID.randomUUID.toString, desc)
      sender ! JobAccepted(job.id)

      val shortJobId = job.id.take(8)
      log.info("job {} accepted ({})", shortJobId, job.id)

      // used later for spinning up instances
      // val nodes = desc.nodes.getOrElse(JobDescriptor.DefaultWorkersPerNode)

      val wpn = JobDescriptor.DefaultWorkersPerNode
      log.info("job will have [{}] workers on each of [{}] cluster nodes", wpn, backend.size)

      val master = cluster.system.actorOf(
        Master.props(job),
        s"master-$shortJobId"
      )

      backend.values.flatMap { node ⇒
        for {
          _ ← 1 to wpn
          workerId = UUID.randomUUID.toString.take(8)
          ref = cluster.system.actorOf(
            Worker.props(workerId, master, job.id).withDeploy(Deploy(scope = RemoteScope(node.address))),
            s"worker-$shortJobId-$workerId"
          )
        } yield workerId → ref
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
