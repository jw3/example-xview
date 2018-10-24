package com.github.jw3.xview.common
import java.nio.file.{Files, Path}

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.stream.alpakka.s3.impl.ListBucketVersion2
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import akka.stream.scaladsl.{FileIO, JsonFraming, Sink, Source}
import akka.util.ByteString
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.AwsRegionProvider
import com.github.jw3.xview.common.MakeChips.{FChip, FeatureData, _}
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

  def chippedFile(t: Int, fid: Int, path: Option[String]): String = path match {
    case Some(p) ⇒ s"$p/$t.$fid.tif"
    case None ⇒ s"$t.$fid.tif"
  }
  def tifFile(t: Int, path: Option[String]): String = path match {
    case Some(p) ⇒ s"$p/$t.tif"
    case None ⇒ s"$t.tif"
  }
  def jsonFile(t: Int, path: Option[String]): String = tifFile(t, path) + ".geojson"

  def number(num: Int, from: S3Path, to: S3Path, filter: Seq[Int] = Seq.empty, ref: ActorRef = ActorRef.noSender)(
      implicit system: ActorSystem,
      mat: Materializer): Future[Done] = {

    val region = new AwsRegionProvider { def getRegion: String = "us-east-1" }
    val credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials("defaultkey", "defaultkey"))
    val endpointOverride = Some(sys.env.getOrElse("S3_URI", "http://localhost:9000"))

    val settings =
      new S3Settings(MemoryBufferType, None, credentials, region, true, endpointOverride, ListBucketVersion2)
    val s3Client = new S3Client(settings)

    import mat.executionContext

    def dl_tif(path: Path) =
      s3Client
        .download(from.bucket, tifFile(num, from.path))
        ._1
        .runWith(FileIO.toPath(path))

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
        .mapAsync(1)(t ⇒
          Source
            .single(t._2)
            .runWith(s3Client.multipartUpload(to.bucket, chippedFile(num, t._1.data.feature_id, to.path))))
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

    val tmp = Files.createTempFile(s"xview-$num", s"tif")
    dl_tif(tmp).map(_ ⇒ GeoTiffReader.readMultiband(tmp.toString)).flatMap(chips)
  }
}
