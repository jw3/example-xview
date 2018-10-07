package com.github.jw3.xview

import java.nio.file.{Path, Paths}
import java.time.{Duration, Instant}

import com.github.jw3.xview.ExampleUtils._
import com.typesafe.scalalogging.LazyLogging
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.io.geotiff.{AutoHigherResolution, GeoTiffMultibandTile, MultibandGeoTiff}
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.{CellSize, RasterExtent}
import geotrellis.vector.io.json.FeatureFormats._
import geotrellis.vector.io.json.GeoJson
import geotrellis.vector.io.json.GeometryFormats._
import geotrellis.vector.{Feature, Polygon}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class FeatureData(feature_id: Int, type_id: Int)
object FeatureData {
  implicit val format: RootJsonFormat[FeatureData] = jsonFormat2(FeatureData.apply)
}

object Example extends App with LazyLogging {
  implicit val wd: Path = Paths.get(sys.env.getOrElse("WORKING_DIR", sys.env.getOrElse("HOME", "/tmp")))
  val sourceTile = 100

  val start = Instant.now

  GeoJson
    .fromFile[List[Feature[Polygon, FeatureData]]](s"$wd/data/$sourceTile.geojson")
    .zipWithIndex
    .foreach { f ⇒
      val fid = f._1.data.feature_id
      val ftype = f._1.data.type_id
      val chipExtent = f._1.geom.envelope

      {
        val idx = f._2.toString.padTo(4, " ").mkString
        println(s"$idx\t$ftype\t$fid")
      }

      // read in a tif
      val tiff: MultibandGeoTiff = GeoTiffReader.readMultiband(s"$wd/data/$sourceTile.tif")

      // create a tile
      val tile = GeoTiffMultibandTile.apply(tiff.tile)

      ///// chip

      // crop to the chip extent
      val chip = GeoTiffMultibandTile(
        tile.crop(tiff.extent, chipExtent)
      )

      // back to a tiff and write
      val chipTiff = MultibandGeoTiff(chip, chipExtent, tiff.crs)
      GeoTiffWriter.write(
        chipTiff,
        s"$wd/chips/$fid-$ftype-chip.tif"
      )

      //// scale

      writeZoomed(s"$fid-$ftype-large.chip", chipTiff)(_.zoomIn())
      writeZoomed(s"$fid-$ftype-small.chip", chipTiff)(_.zoomOut())
    }

  println(Duration.between(start, Instant.now))
}

object ExampleUtils {
  def writeZoomed(fname: String, tiff: MultibandGeoTiff)(z: CellSize ⇒ CellSize)(implicit wd: Path): Unit = {
    // scale and resample the raster
    val zoomed = tiff.resample(
      RasterExtent(tiff.extent, z(tiff.cellSize)),
      NearestNeighbor,
      AutoHigherResolution
    )

    // back to a tiff and write
    GeoTiffWriter.write(
      MultibandGeoTiff(zoomed.tile, zoomed.extent, tiff.crs),
      s"$wd/chips/$fname.tif"
    )
  }

  implicit class CellSizeOps(cs: CellSize) {
    def zoomIn(f: Int = 2): CellSize = CellSize(cs.width / f, cs.height / f)
    def zoomOut(f: Int = 2): CellSize = CellSize(cs.width * f, cs.height * f)
  }
}
