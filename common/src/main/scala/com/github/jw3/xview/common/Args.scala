package com.github.jw3.xview.common

object Args {
  case class S3Args(tile: Int, source: S3Path, target: S3Path)

  def s3Paths(t: Int, s: String, d: String): S3Args = {
    val (sb, sp) = {
      val ss = s.split("/", 2)
      if (ss.size > 1) (ss.head, ss.lastOption)
      else (ss.head, None)
    }
    val (db, dp) = {
      val ds = d.split("/", 2)
      if (ds.size > 1) (ds.head, ds.lastOption)
      else (ds.head, None)
    }

    S3Args(t, S3Path(sb, sp), S3Path(db, dp))
  }
}
