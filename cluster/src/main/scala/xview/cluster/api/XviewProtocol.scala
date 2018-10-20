package xview.cluster.api

import spray.json._

case class Job(id: String, tiles: Seq[Int], classes: Option[Seq[Int]])
object Job extends DefaultJsonProtocol {
  def apply(id: String, desc: JobDescriptor): Job = Job(id, desc.tiles, desc.classes)
}

case class JobDescriptor(tiles: Seq[Int], classes: Option[Seq[Int]], nodes: Option[Int])
object JobDescriptor extends DefaultJsonProtocol {
  implicit val format: RootJsonFormat[JobDescriptor] = jsonFormat3(JobDescriptor.apply)

  val DefaultNodeCount = 3
  val DefaultWorkersPerNode = 3
}

case class SubmitJob(desc: JobDescriptor)

// http api
sealed trait SubmitJobResponse
case class JobAccepted(id: String) extends SubmitJobResponse
case object JobRejected extends SubmitJobResponse

// master-worker api
case class Task(tile: Int, filter: Option[Seq[Int]] = None)
case class RequestTasking(worker: String)
case class TaskCompleted(worker: String, tile: Int)
case class TaskFailed(worker: String, tile: Int)

case class RegisterWorker(worker: String)
case class DeregisterWorker(worker: String)
