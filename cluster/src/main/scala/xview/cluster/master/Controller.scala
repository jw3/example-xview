package xview.cluster.master

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Deploy, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member}
import akka.remote.RemoteScope
import xview.cluster.Roles
import xview.cluster.api.{JobAccepted, SubmitJob, _}
import xview.cluster.master.Controller._
import xview.cluster.worker.Worker

import scala.collection.Map

object Controller {
  def props() = Props(new Controller)
  def shortId(id: String) = id.take(8)
}

//
// todo;; eventually members will contain job context in their roles
//     ;; to allow elasticity and prevent actors bleeding across nodes
//
class Controller extends Actor with ActorLogging {
  val cluster: Cluster = Cluster(context.system)
  var backend: Map[String, Member] = Map.empty
  var masters: Map[String, ActorRef] = Map.empty
  val wpn = JobDescriptor.DefaultWorkersPerNode

  // todo;; hack for now till figure out appropriate states
  var executingJob: Option[Job] = None

  def receive: Receive = {
    case SubmitJob(_) if backend.isEmpty ⇒
      sender ! JobRejected

    case SubmitJob(desc) ⇒
      val job = Job(UUID.randomUUID.toString, desc)
      sender ! JobAccepted(job.id)

      val shortJobId = shortId(job.id)
      log.info("job {} accepted ({})", shortJobId, job.id)

      // used later for spinning up instances
      // val nodes = desc.nodes.getOrElse(JobDescriptor.DefaultNodeCount)
      log.info("job will have [{}] workers on each of [{}] cluster nodes", wpn, backend.size)

      val master = cluster.system.actorOf(
        Master.props(job),
        s"master-$shortJobId"
      )
      masters += job.id → master

      backend.values.flatMap(populateNode(wpn, _, job))
      executingJob = Some(job)

    case MemberUp(member) ⇒
      if (member.roles.contains(Roles.Worker)) {
        log.info("=====> backend node up: {}", member.address)
        backend += member.address.toString → member

        // todo;; hack for now till figure out appropriate states
        executingJob.map(populateNode(wpn, member, _))
      }

    case UnreachableMember(member) ⇒
      log.info("backend node unreachable, removing {}", member)
      backend = backend.filterNot(e ⇒ e._2 == member)

    case MemberRemoved(member, _) ⇒
      log.info("backend node removed, {}", member)
      backend = backend.filterNot(e ⇒ e._2 == member)

    case _: MemberEvent ⇒
  }

  def populateNode(wpn: Int, node: Member, job: Job): Seq[(String, ActorRef)] = {
    val master = masters(job.id)
    for {
      _ ← 1 to wpn
      workerId = UUID.randomUUID.toString.take(8)
      ref = cluster.system.actorOf(
        Worker.props(workerId, master, job.id).withDeploy(Deploy(scope = RemoteScope(node.address))),
        s"worker-${shortId(job.id)}-$workerId"
      )
    } yield workerId → ref
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
