package com.github.jw3.xview

import com.amazonaws.regions.RegionUtils
import com.amazonaws.regions.Region

sealed trait S3Config {
  def region: Region
  def accessKey: Option[String]
  def secretKey: Option[String]
  def endpoint: Option[String]
}

object S3Config {
  def apply(r: String): S3Config = new S3Config() {
    val region: Region = RegionUtils.getRegion(r)
    val accessKey: Option[String] = None
    val secretKey: Option[String] = None
    val endpoint: Option[String] = None
  }

  def apply(r: String, ak: String, sk: String): S3Config = new S3Config {
    val region: Region = RegionUtils.getRegion(r)
    def accessKey: Option[String] = Some(ak)
    def secretKey: Option[String] = Some(sk)
    def endpoint: Option[String] = None
  }

  def local(ak: String, sk: String): S3Config = new S3Config {
    val region: Region = RegionUtils.getRegion("us-east-1")
    def accessKey: Option[String] = Some(ak)
    def secretKey: Option[String] = Some(sk)
    def endpoint: Option[String] = Some("http://localhost:3000")
  }
}
