package com.github.jw3.xview

import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import com.github.jw3.xview.common.MakeChips.{FChip, FeatureData, _}
import com.github.jw3.xview.common.{S3ClientStream, S3Config}
import com.typesafe.scalalogging.LazyLogging
import geotrellis.raster.io.geotiff.MultibandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.io.json.FeatureFormats._
import geotrellis.vector.io.json.GeoJson
import geotrellis.vector.io.json.GeometryFormats._
import geotrellis.vector.{Feature, Polygon}
import spray.json.DefaultJsonProtocol._

object CLI {}

object Chip extends App with LazyLogging {
  implicit val system: ActorSystem = ActorSystem("chipper")
  implicit val materializer: Materializer = ActorMaterializer()

  implicit val wd: Path = Paths.get(sys.env.getOrElse("CHIP_FROM", sys.env.getOrElse("HOME", "/tmp")))
  implicit val cfg: S3Config = S3Config.local("defaultkey", "defaultkey")

  val zooms = false
  val filter = true

  val (Some(tilenum), Some(bucket), prefix) =
    args match {
      case Array(t, b) ⇒ (Some(t), Some(b), None)
      case Array(t, b, p) ⇒ (Some(t), Some(b), Some(p))
      case Array("-cfg") ⇒
        println(s"v${BuildInfo.version}")
        println(s"working directory set to $wd")
        println(s"s3 endpoint set to ${cfg.endpoint}")
        sys.exit(1)
      case _ ⇒
        args.foreach(println)
        println("usage: chip <tile-number> <bucket> [prefix]")
        sys.exit(1)
    }

  val tif_file = wd.resolve(s"$tilenum.tif").toString
  val json_file = wd.resolve(s"$tilenum.tif.geojson").toString

  logger.info("chipping tile {} [{}]", tilenum, tif_file)

  Source
    .fromIterator(
      () ⇒ GeoJson.fromFile[List[Feature[Polygon, FeatureData]]](json_file.toString).iterator
    )
    .filter(f ⇒ !filter || (Seq(18, 73) contains f.data.type_id))
    .statefulMapConcat { () ⇒
      {
        val tif: MultibandGeoTiff = GeoTiffReader.readMultiband(tif_file)
        f ⇒
          {
            val cropped = crop(FChip(f, tif))

            if (zooms) {
              val zoomedIn = cropped.flatMap(zoom(_, "large")(_.zoomIn()))
              val zoomedOut = cropped.flatMap(zoom(_, "small")(_.zoomOut()))

              cropped ::: zoomedIn ::: zoomedOut ::: Nil
            } else cropped
          }
      }
    }
    .mapAsync(4) { chip ⇒
      val path = (prefix, chip.name, chip.f.data.feature_id, chip.f.data.type_id) match {
        case (None, None, fid, t) ⇒ s"$t/$fid.png"
        case (Some(p), None, fid, t) ⇒ s"$p/$t/$fid.png"
        case (None, Some(n), fid, t) ⇒ s"$t/$fid.$n.png"
        case (Some(p), Some(n), fid, t) ⇒ s"$p/$t/$fid.$n.png"
      }

      println(s"upload $path")
      S3ClientStream().multipartUpload(bucket, path) {
        Source.single(ByteString(chip.t.tile.renderPng.bytes))
      }
    }
    .runWith(Sink.ignore)
    .onComplete { _ ⇒
      system.terminate()
    }(system.dispatcher)
}
