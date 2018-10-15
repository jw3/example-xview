package com.github.jw3.xview

import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import com.github.jw3.xview.MakeChips._
import com.typesafe.scalalogging.LazyLogging
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.{AutoHigherResolution, GeoTiffOptions, MultibandGeoTiff}
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.{CellSize, RasterExtent}
import geotrellis.vector.io.json.FeatureFormats._
import geotrellis.vector.io.json.GeoJson
import geotrellis.vector.io.json.GeometryFormats._
import geotrellis.vector.{Feature, Polygon}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object MakeChips {
  type PolygonFeature = Feature[Polygon, FeatureData]
  case class FChip(f: PolygonFeature, t: MultibandGeoTiff, name: Option[String] = None)

  case class FeatureData(feature_id: Int, type_id: Int, image_id: String)
  object FeatureData {
    implicit val format: RootJsonFormat[FeatureData] = jsonFormat3(FeatureData.apply)
  }

  def crop(in: FChip): List[FChip] = {
    val f = in.f
    val tiff = in.t
    val chipOpts = GeoTiffOptions.DEFAULT.copy(colorSpace = tiff.options.colorSpace)
    List(
      FChip(in.f, MultibandGeoTiff(tiff.tile.crop(tiff.extent, f.geom.envelope), f.geom.envelope, tiff.crs, chipOpts))
    )
  }

  def zoom(in: FChip, name: String)(z: CellSize ⇒ CellSize): List[FChip] = {
    val f = in.f
    val tiff = in.t
    val zoomed = tiff.resample(
      RasterExtent(tiff.extent, z(tiff.cellSize)),
      NearestNeighbor,
      AutoHigherResolution
    )
    List(FChip(in.f, MultibandGeoTiff(zoomed, f.geom.envelope, tiff.crs, tiff.options), Some(name)))
  }

  implicit class CellSizeOps(cs: CellSize) {
    def zoomIn(f: Int = 2): CellSize = CellSize(cs.width / f, cs.height / f)
    def zoomOut(f: Int = 2): CellSize = CellSize(cs.width * f, cs.height * f)
  }
}

// cli example
//
// chip tilenum out-bucket/out-dataset-name
//
object Chip extends App with LazyLogging {
  import MakeChips.CellSizeOps

  implicit val system: ActorSystem = ActorSystem("chipper")
  implicit val materializer: Materializer = ActorMaterializer()

  implicit val wd: Path = Paths.get(sys.env.getOrElse("CHIP_FROM", sys.env.getOrElse("HOME", "/tmp")))

  // input
  val tilenum = 128 //args(1).toInt
  val tif_file = wd.resolve(s"$tilenum.tif").toString
  val json_file = wd.resolve(s"$tilenum.tif.geojson").toString

  // output
  val bucket = "foo"
  val prefix = s"1.2/$tilenum"
  implicit val cfg = S3Config.local("defaultkey", "defaultkey")

  logger.info("chipping tile {}", tilenum)

  val source =
    Source
      .fromIterator(
        () ⇒ GeoJson.fromFile[List[Feature[Polygon, FeatureData]]](json_file.toString).iterator
      )
      .statefulMapConcat { () ⇒
        {
          val tif: MultibandGeoTiff = GeoTiffReader.readMultiband(tif_file)
          f ⇒
            {
              val cropped = crop(FChip(f, tif))
              val zoomedIn = cropped.flatMap(zoom(_, "in")(_.zoomIn()))
              val zoomedOut = cropped.flatMap(zoom(_, "out")(_.zoomOut()))

              cropped ::: zoomedIn ::: zoomedOut ::: Nil
            }
        }
      }
      .mapAsync(4) { chip ⇒
        val path = (prefix, chip.name, chip.f.data.feature_id, chip.f.data.type_id) match {
          case (p, None, fid, t) ⇒ s"$p/$t/$fid.png"
          case (p, Some(n), fid, t) ⇒ s"$p/$t/$fid.$n.png"
        }

        S3ClientStream().multipartUpload(bucket, path) {
          Source.single(ByteString(chip.t.tile.renderPng.bytes))
        }
      }
      .runWith(Sink.ignore)
}
