package com.github.jw3.xview.common
import spray.json.DefaultJsonProtocol

case class KittiLabel(id: String, minx: Double, miny: Double, maxx: Double, maxy: Double)
object KittiLabel extends DefaultJsonProtocol {
  def toRow(kl: KittiLabel): String =
    s"${kl.id} 0.0 0 0.0 ${kl.minx} ${kl.miny} ${kl.maxx} ${kl.maxy} 0.0 0.0 0.0 0.0 0.0 0.0 0.0"
}
