package com.github.jw3.xview.common
import java.nio.file.{Files, Path}

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.stream.alpakka.s3.S3Settings
import akka.stream.alpakka.s3.impl.S3Headers
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{FileIO, JsonFraming, Sink, Source}
import akka.util.ByteString
import com.github.jw3.xview.common.MakeChips.{FChip, FeatureData, _}
import geotrellis.raster.RasterExtent
import geotrellis.raster.io.geotiff.MultibandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.io.json.FeatureFormats._
import geotrellis.vector.io.json.GeoJson
import geotrellis.vector.io.json.GeometryFormats._
import geotrellis.vector.{Feature, Polygon}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ProcessTile {
  case class Complete(id: Int)

  def chippedFile(t: Int, fid: Int, ext: String, path: Option[String]): String = path match {
    case Some(p) ⇒ s"$p/$t.$fid.$ext"
    case None ⇒ s"$t.$fid.$ext"
  }
  def chippedFileWithType(t: Int, fid: Int, ext: String, ftype: Int, path: Option[String]): String = path match {
    case Some(p) ⇒ chippedFile(t, fid, ext, Some(s"$p/$ftype"))
    case None ⇒ chippedFile(t, fid, ext, Some(s"$ftype"))
  }
  def tifFile(t: Int, path: Option[String]): String = path match {
    case Some(p) ⇒ s"$p/$t.tif"
    case None ⇒ s"$t.tif"
  }
  def jsonFile(t: Int, path: Option[String]): String = tifFile(t, path) + ".geojson"

  def labelFile(t: Int, path: Option[String]): String = path match {
    case Some(p) ⇒ s"$p/$t.txt"
    case None ⇒ s"$t.txt"
  }

  def dl_tif(tile: Int, from: S3Path, to: Path)(implicit mat: Materializer, s3: S3Client) =
    s3.download(from.bucket, tifFile(tile, from.path))._1.runWith(FileIO.toPath(to))

  def number(num: Int, from: S3Path, to: S3Path, filter: Seq[Int] = Seq.empty, ref: ActorRef = ActorRef.noSender)(
      implicit system: ActorSystem,
      mat: Materializer): Future[Done] = {

    import system.dispatcher

    implicit val s3Client = new S3Client(S3Settings.create(system))

    def chips(tif: MultibandGeoTiff) = {
      val f = s3Client
        .download(from.bucket, jsonFile(num, from.path))
        ._1
        .via(JsonFraming.objectScanner(1024))
        .map(bs ⇒ GeoJson.parse[Feature[Polygon, FeatureData]](bs.utf8String))
        .filter(f ⇒ filter.isEmpty || filter.contains(f.data.type_id))
        .mapConcat { fd ⇒
          crop(FChip(fd, tif))
        }
        .map(c ⇒ c.f → ByteString(c.t.tile.renderPng().bytes))
        .mapAsync(1)(
          t ⇒
            Source
              .single(t._2)
              .runWith(s3Client
                .multipartUpload(to.bucket,
                                 chippedFileWithType(num, t._1.data.feature_id, "png", t._1.data.type_id, to.path))))
        .runWith(Sink.ignore)
      f.onComplete {
        case Success(_) if ref != ActorRef.noSender ⇒ ref ! Complete(num)
        case f @ Failure(_) if ref != ActorRef.noSender ⇒ ref ! f
        case Success(_) ⇒ println("success")
        case f @ Failure(e) ⇒
          e.printStackTrace()
          println("failure")
      }

      f
    }

    def labels(tif: MultibandGeoTiff): Future[Done] = {
      val f = s3Client
        .download(from.bucket, jsonFile(num, from.path))
        ._1
        .via(JsonFraming.objectScanner(1024))
        .map(bs ⇒ GeoJson.parse[Feature[Polygon, FeatureData]](bs.utf8String))
        .filter(f ⇒ filter.isEmpty || filter.contains(f.data.type_id))
        .map { fd ⇒
          val re = RasterExtent(tif.extent, tif.cellSize)
          val env = fd.geom.envelope
          val min = re.mapToGrid(env.xmin, env.ymin)
          val max = re.mapToGrid(env.xmax, env.ymax)

          val l = YoloLabel(s"${fd.data.type_id}", min._1, min._2, max._1, max._2)
          FLabel(fd, YoloLabel.toRow(l))
        }
        .scan(StringBuilder.newBuilder) { (x, y) ⇒
          x ++= y.l
        }
        .map(s ⇒ ByteString(s.mkString))
        .mapAsync(1)(t ⇒
          s3Client
            .putObject(to.bucket, labelFile(num, to.path), Source.single(t), t.length, s3Headers = S3Headers.empty))
        .runWith(Sink.ignore)
      f.onComplete {
        case Success(_) if ref != ActorRef.noSender ⇒ ref ! Complete(num)
        case f @ Failure(_) if ref != ActorRef.noSender ⇒ ref ! f
        case Success(_) ⇒ println("success")
        case f @ Failure(e) ⇒
          e.printStackTrace()
          println("failure")
      }

      f
    }

    for {
      tmp ← Future(Files.createTempFile(s"xview-$num", s".tif"))
      tif ← dl_tif(num, from, tmp).map(_ ⇒ GeoTiffReader.readMultiband(tmp.toString))
      _ ← chips(tif)
      _ ← labels(tif)
    } yield Done
  }
}
