package xview.cluster.api

import spray.json._

case class Job(tiles: Seq[Int], cats: Option[Seq[Int]])
object Job extends DefaultJsonProtocol {
  def apply(tiles: Seq[Int]): Job = Job(tiles, None)
  def apply(tiles: Seq[Int], cats: Seq[Int]): Job = Job(tiles, Some(cats))

  val DefaultWorkerCount = 3
  implicit val format: RootJsonFormat[Job] = jsonFormat2(Job.apply(_: Seq[Int], _: Option[Seq[Int]]))
}

case class JobDescriptor(job: Job, workers: Option[Int])
object JobDescriptor extends DefaultJsonProtocol {
  implicit val format: RootJsonFormat[JobDescriptor] = jsonFormat2(JobDescriptor.apply)
}

case class SubmitJob(job: Job, workers: Int)

sealed trait SubmitJobResponse
case object JobAccepted extends SubmitJobResponse
case object JobRejected extends SubmitJobResponse

case class WorkStarted(tile: Int)
case class WorkCompleted(tile: Int)
case class WorkFailed(tile: Int)

case object RegisterWorker
