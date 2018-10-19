package xview.cluster.api

case class Job(tiles: Seq[Int], cats: Seq[Int])

case class StartJob(job: Job, workers: Int = 3)
case class StartWork(tile: Int)
case class FinishedWork(tile: Int)

case object RegisterWorker
