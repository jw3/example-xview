package com.github.jw3.xview

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.github.jw3.xview.common.{Args, ProcessTile, S3Path}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object ChipS3 extends App with LazyLogging {
  implicit val system: ActorSystem = ActorSystem("chipper")
  implicit val materializer: Materializer = ActorMaterializer()

  val zooms = false
  val filter = true

  val (Some(tilenum: Int), Some(sourceBucket), sourcePrefix, Some(destBucket), destPrefix) =
    args match {
      case Array(t, s, d) ⇒
        val s3p = Args.s3Paths(t.toInt, s, d)
        (Some(s3p.tile), Some(s3p.source.bucket), s3p.source.path, Some(s3p.target.bucket), s3p.target.path)

      case Array("-cfg") ⇒
        println(s"v${BuildInfo.version}")
        sys.env.get("AWS_S3_ENDPOINT").foreach(ep ⇒ println(s"s3 endpoint set to $ep"))
        sys.exit(1)
      case _ ⇒
        args.foreach(println)
        println("usage: chip <tile-number> <src-bucket>[/prefix] <dst-bucket>[/prefix]")
        sys.exit(1)
    }

  logger.info(s"chipping tile [$tilenum] from $sourceBucket [$sourcePrefix] to $destBucket [$destPrefix]")

  import system.dispatcher
  val res = ProcessTile.number(tilenum, S3Path(sourceBucket, sourcePrefix), S3Path(destBucket, destPrefix))

  res.onComplete {
    case Success(_) ⇒
      logger.info(s"$tilenum complete")
    case Failure(ex) ⇒
      logger.error(s"$tilenum failed", ex)
  }
  res.onComplete(_ ⇒ system.terminate())

  Await.ready(res, Duration.Inf)
}
