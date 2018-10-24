package com.github.jw3.xview.common

case class S3Path(bucket: String, path: Option[String])
object S3Path {
  def apply(bucket: String, path: String): S3Path = S3Path(bucket, Some(path))
}
