package xview.cluster.worker

import java.nio.file.Path

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.stream.alpakka.s3.impl.ListBucketVersion2
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.AwsRegionProvider
import com.github.jw3.xview.MakeChips.{crop, FChip, FeatureData}
import com.github.jw3.xview.S3Config
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.io.json.FeatureFormats._
import geotrellis.vector.io.json.GeoJson
import geotrellis.vector.io.json.GeometryFormats._
import geotrellis.vector.{Feature, Polygon}
import spray.json.DefaultJsonProtocol._
import xview.cluster.api.S3Path

import scala.util.{Failure, Success}

object ProcessTile {
  case class Complete(id: Int)

  def tifFile(t: Int, wd: Path): String = wd.resolve(s"$t.tif").toString
  def jsonFile(t: Int, wd: Path): String = wd.resolve(s"$t.tif.geojson").toString
  implicit val cfg: S3Config = S3Config.local("defaultkey", "defaultkey")

  def number(num: Int, from: Path, to: S3Path, filter: Seq[Int] = Seq.empty, ref: ActorRef = ActorRef.noSender)(
      implicit sys: ActorSystem,
      mat: Materializer) = {

    val awsCredentialsProvider = new AWSStaticCredentialsProvider(
      new BasicAWSCredentials("defaultkey", "defaultkey")
    )
    val regionProvider =
      new AwsRegionProvider {
        def getRegion: String = "us-east-1"
      }

    val settings =
      new S3Settings(MemoryBufferType, None, awsCredentialsProvider, regionProvider, false, None, ListBucketVersion2)
    val s3Client = new S3Client(settings)(sys, mat)

    import mat.executionContext

    s3Client
      .download("xview", s"training_images/${jsonFile(num, from)}")
      ._1
      .mapConcat(bs ⇒ GeoJson.parse[List[Feature[Polygon, FeatureData]]](bs.utf8String))
      .filter(f ⇒ filter.isEmpty || filter.contains(f.data.type_id))
      .mapAsync(1)(
        fd ⇒
          s3Client
            .download("xview", s"training_images/${tifFile(num, from)}")
            ._1
            .runWith(Sink.head)
            .map(fd → _))
      .map(bs ⇒ bs._1 → GeoTiffReader.readMultiband(bs._2.toByteBuffer))
      .mapConcat { fd ⇒
        crop(FChip(fd._1, fd._2))
      }
      .map(c ⇒ ByteString(c.t.toByteArray))
      .runWith(s3Client.multipartUpload(to.bucket, to.path.getOrElse("")))
      .onComplete {
        case Success(_) ⇒ ref ! Complete(num)
        case f @ Failure(_) ⇒ ref ! f
      }

  }
}
