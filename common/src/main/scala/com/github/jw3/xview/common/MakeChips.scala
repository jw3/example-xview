package com.github.jw3.xview.common

import geotrellis.raster.{CellSize, RasterExtent}
import geotrellis.raster.io.geotiff.{AutoHigherResolution, GeoTiffOptions, MultibandGeoTiff}
import geotrellis.raster.resample.NearestNeighbor
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
    val chipOpts = GeoTiffOptions.DEFAULT.copy(colorSpace = in.t.options.colorSpace)
    List(
      FChip(in.f,
            MultibandGeoTiff(in.t.tile.crop(in.t.extent, in.f.geom.envelope), in.f.geom.envelope, in.t.crs, chipOpts))
    )
  }

  def zoom(in: FChip, name: String)(z: CellSize ⇒ CellSize): List[FChip] = {
    val zoomed = in.t.resample(
      RasterExtent(in.t.extent, z(in.t.cellSize)),
      NearestNeighbor,
      AutoHigherResolution
    )
    List(FChip(in.f, MultibandGeoTiff(zoomed, in.f.geom.envelope, in.t.crs, in.t.options), Some(name)))
  }

  implicit class CellSizeOps(cs: CellSize) {
    def zoomIn(f: Int = 2): CellSize = CellSize(cs.width / f, cs.height / f)
    def zoomOut(f: Int = 2): CellSize = CellSize(cs.width * f, cs.height * f)
  }
}
