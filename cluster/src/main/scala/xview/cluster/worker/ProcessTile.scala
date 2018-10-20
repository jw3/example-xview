package xview.cluster.worker

import java.nio.file.Path

import akka.actor.ActorContext
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.github.jw3.xview.MakeChips.{crop, FChip, FeatureData}
import com.github.jw3.xview.{S3ClientStream, S3Config}
import geotrellis.raster.io.geotiff.MultibandGeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.io.json.FeatureFormats._
import geotrellis.vector.io.json.GeoJson
import geotrellis.vector.io.json.GeometryFormats._
import geotrellis.vector.{Feature, Polygon}
import spray.json.DefaultJsonProtocol._
import xview.cluster.api.S3Path

object ProcessTile {
  case class Complete(id: Int)

  def tifFile(t: Int, wd: Path): String = wd.resolve(s"$t.tif").toString
  def jsonFile(t: Int, wd: Path): String = wd.resolve(s"$t.tif.geojson").toString
  implicit val cfg: S3Config = S3Config.local("defaultkey", "defaultkey")

  def number(num: Int, from: Path, to: S3Path, filter: Seq[Int] = Seq.empty)(implicit ctx: ActorContext,
                                                                             mat: Materializer) =
    Source
      .fromIterator(
        () ⇒ GeoJson.fromFile[List[Feature[Polygon, FeatureData]]](jsonFile(num, from)).iterator
      )
      .filter(f ⇒ filter.isEmpty || filter.contains(f.data.type_id))
      .statefulMapConcat { () ⇒
        {
          val tif: MultibandGeoTiff = GeoTiffReader.readMultiband(tifFile(num, from))
          f ⇒
            crop(FChip(f, tif))
        }
      }
      .mapAsync(4) { chip ⇒
        S3ClientStream().multipartUpload(to.bucket, to.path.getOrElse("")) {
          Source.single(ByteString(chip.t.tile.renderPng.bytes))
        }
      }
      .runWith(Sink.actorRef(ctx.self, Complete(num)))
}
