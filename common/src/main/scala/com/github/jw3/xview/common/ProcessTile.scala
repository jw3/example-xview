package com.github.jw3.xview.common
import java.nio.file.{Files, Path}

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.stream.alpakka.s3.S3Settings
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{FileIO, JsonFraming, Sink, Source}
import akka.util.ByteString
import com.github.jw3.xview.common.MakeChips.{FChip, FeatureData, _}
import geotrellis.raster
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
        .map(c ⇒ c.f → ByteString(c.t.toByteArray))
        .mapAsync(1)(
          t ⇒
            Source
              .single(t._2)
              .runWith(s3Client
                .multipartUpload(to.bucket,
                                 chippedFileWithType(num, t._1.data.feature_id, "tif", t._1.data.type_id, to.path))))
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
          val re = raster.RasterExtent(tif.tile, tif.extent)
          val ext = fd.geom.envelope
          val l = KittiLabel(s"${fd.data.type_id}",
                             re.mapXToGrid(ext.xmin),
                             re.mapYToGrid(ext.ymin),
                             re.mapXToGrid(ext.xmax),
                             re.mapYToGrid(ext.ymax))
          println(l)
          FLabel(fd, KittiLabel.toRow(l))
        }
        .map(c ⇒ c.f → ByteString(c.l))
        .mapAsync(1)(
          t ⇒
            Source
              .single(t._2)
              .runWith(s3Client
                .multipartUpload(to.bucket,
                                 chippedFileWithType(num, t._1.data.feature_id, "txt", t._1.data.type_id, to.path))))
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
