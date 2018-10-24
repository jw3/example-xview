package com.github.jw3.xview

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.github.jw3.xview.common.{ProcessTile, S3Config, S3Path}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object ChipS3 extends App with LazyLogging {
  implicit val system: ActorSystem = ActorSystem("chipper")
  implicit val materializer: Materializer = ActorMaterializer()

  implicit val cfg: S3Config = S3Config.local("defaultkey", "defaultkey")

  val zooms = false
  val filter = true

  val (Some(tilenum: Int), Some(bucket), prefix) =
    args match {
      case Array(t, b) ⇒ (Some(t.toInt), Some(b), None)
      case Array(t, b, p) ⇒ (Some(t.toInt), Some(b), Some(p))
      case Array("-cfg") ⇒
        println(s"v${BuildInfo.version}")
        println(s"s3 endpoint set to ${cfg.endpoint}")
        sys.exit(1)
      case _ ⇒
        args.foreach(println)
        println("usage: chip <tile-number> <bucket> [prefix]")
        sys.exit(1)
    }

  logger.info(s"chipping tile [$tilenum] from ")

  import system.dispatcher
  val res = ProcessTile.number(tilenum, S3Path(bucket, prefix), S3Path(bucket, "chips"))

  res.onComplete {
    case Success(_) ⇒
      logger.info(s"$tilenum complete")
    case Failure(ex) ⇒
      logger.error(s"$tilenum failed", ex)
  }
  res.onComplete(_ ⇒ system.terminate())

  Await.ready(res, Duration.Inf)
}
