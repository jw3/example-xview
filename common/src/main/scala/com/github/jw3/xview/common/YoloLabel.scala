package com.github.jw3.xview.common
import spray.json.DefaultJsonProtocol

case class YoloLabel(id: String, x: Int, y: Int, w: Int, h: Int)
object YoloLabel extends DefaultJsonProtocol {
  def toRow(l: YoloLabel): String = s"${l.id} ${l.x} ${l.y} ${l.w} ${l.h}"
}
