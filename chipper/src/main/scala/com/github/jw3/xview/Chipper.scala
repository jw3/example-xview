package com.github.jw3.xview

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path, Paths}
import java.time.{Duration, Instant}

import akka.actor.ActorSystem
import akka.stream.scaladsl.StreamConverters
import akka.stream.{ActorMaterializer, Materializer}
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult
import com.github.jw3.xview.ExampleUtils._
import com.typesafe.scalalogging.LazyLogging
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.{AutoHigherResolution, GeoTiffMultibandTile, GeoTiffOptions, MultibandGeoTiff}
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.{CellSize, RasterExtent}
import geotrellis.vector.io.json.FeatureFormats._
import geotrellis.vector.io.json.GeoJson
import geotrellis.vector.io.json.GeometryFormats._
import geotrellis.vector.{Feature, Polygon}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future



// cli example
//
// chip ~/100.tif wassj/train
//
object Chipper extends App with LazyLogging {
  implicit val system: ActorSystem = ActorSystem("chipper")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val wd: Path = Paths.get(sys.env.getOrElse("WORKING_DIR", sys.env.getOrElse("HOME", "/tmp")))
  val geojson = wd.resolve("data/100.geojson")
  val bucket = "wassj"

  logger.info("loading features from {}", geojson)

  val start = Instant.now

  GeoJson
    .fromFile[List[Feature[Polygon, FeatureData]]](geojson.toString)
    .groupBy(_.data.image_id)
    .foreach { t ⇒
      logger.info("{} chips in {}", Int.box(t._2.size), t._1)

      // read in a tif
      val tiff: MultibandGeoTiff = GeoTiffReader.readMultiband(s"$wd/data/${t._1}")
      // create a tile
      val tile = GeoTiffMultibandTile(tiff.tile)
      // generate chips
      t._2.zipWithIndex.par.foreach(f ⇒ chipFeature(f._2, f._1, tiff, tile))
    }

  println(Duration.between(start, Instant.now))
}

object ExampleUtils {
  case class FeatureData(feature_id: Int, type_id: Int, image_id: String)
  object FeatureData {
    implicit val format: RootJsonFormat[FeatureData] = jsonFormat3(FeatureData.apply)
  }

  def chipFeature(idx: Int, f: Feature[Polygon, FeatureData], tiff: MultibandGeoTiff, tile: GeoTiffMultibandTile)(
      implicit wd: Path,
      mat: Materializer): Future[Unit] = {

    import mat.executionContext

    val fid = f.data.feature_id
    val ftype = f.data.type_id
    val chipExtent = f.geom.envelope

    {
      val idxStr: String = "% 4d".format(idx)
      println(s"$idxStr\t$ftype\t$fid")
    }

    ///// chip

    // crop to the chip extent
    val chip = GeoTiffMultibandTile(
      tile.crop(tiff.extent, chipExtent)
    )

    val out = Paths.get(s"wassj/test4/$ftype")
    Files.createDirectories(out)

    // back to a tiff and write w/ same color as original
    val chipOpts = GeoTiffOptions.DEFAULT.copy(colorSpace = tiff.options.colorSpace)
    val chipTiff = MultibandGeoTiff(chip, chipExtent, tiff.crs, chipOpts)
//    GeoTiffWriter.write(
//      chipTiff,
//      out.resolve(s"$fid.tif").toString
//    )

    implicit val cfg = S3Config.local("defaultkey", "defaultkey")
    for {
      _ ← S3ClientStream().multipartUpload(out.toString, s"$fid.png") {
        StreamConverters.fromInputStream(() ⇒ new ByteArrayInputStream(chip.renderPng().bytes))
      }
      _ ← writeZoomed(s"$fid.large.png", out, chipTiff)(_.zoomIn())
      _ ← writeZoomed(s"$fid.small.png", out, chipTiff)(_.zoomOut())
    } yield ()
  }

  def writeZoomed(key: String, path: Path, tiff: MultibandGeoTiff)(
      z: CellSize ⇒ CellSize)(implicit cfg: S3Config, mat: Materializer): Future[CompleteMultipartUploadResult] = {

    // scale and resample the raster
    val zoomed = tiff.resample(
      RasterExtent(tiff.extent, z(tiff.cellSize)),
      NearestNeighbor,
      AutoHigherResolution
    )

    S3ClientStream().multipartUpload(path.toString, key) {
      StreamConverters.fromInputStream(() ⇒ new ByteArrayInputStream(zoomed.tile.renderPng().bytes))
    }
  }

  implicit class CellSizeOps(cs: CellSize) {
    def zoomIn(f: Int = 2): CellSize = CellSize(cs.width / f, cs.height / f)
    def zoomOut(f: Int = 2): CellSize = CellSize(cs.width * f, cs.height * f)
  }
}
