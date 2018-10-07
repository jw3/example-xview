package com.github.jw3.xview

import java.nio.file.{Path, Paths}

import com.github.jw3.xview.ExampleUtils._
import com.typesafe.scalalogging.LazyLogging
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.io.geotiff.{AutoHigherResolution, GeoTiffMultibandTile, MultibandGeoTiff}
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.{CellSize, RasterExtent}
import geotrellis.vector.Polygon
import geotrellis.vector.io.wkt.WKT

object Example extends App with LazyLogging {
  implicit val wd: Path = Paths.get(sys.env.getOrElse("WORKING_DIR", sys.env.getOrElse("HOME", "/tmp")))

  // take some wkt from 100 training data 1016347
  val wkt =
    """
      |POLYGON((
      |10.25204021977258684 36.86410345210712336,
      |10.25204021977258684 36.86432007437027636,
      |10.25229550558354141 36.86432007437027636,
      |10.25229550558354141 36.86410345210712336,
      |10.25204021977258684 36.86410345210712336
      |))
    """.stripMargin

  val chipBounds = WKT.read(wkt).asInstanceOf[Polygon]
  val chipExtent = chipBounds.envelope

  // read in a tif
  val tiff: MultibandGeoTiff = GeoTiffReader.readMultiband(s"$wd/100.tif")

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
    s"$wd/100.clipped.tif"
  )

  //// scale

  writeZoomed("100.clipped.large", chipTiff)(_.zoomIn())
  writeZoomed("100.clipped.small", chipTiff)(_.zoomOut())
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
      s"$wd/$fname.tif"
    )
  }

  implicit class CellSizeOps(cs: CellSize) {
    def zoomIn(f: Int = 2): CellSize = CellSize(cs.width / f, cs.height / f)
    def zoomOut(f: Int = 2): CellSize = CellSize(cs.width * f, cs.height * f)
  }
}
