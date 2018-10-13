lazy val `example-xview` =
  project
    .in(file("."))
    .aggregate(chipper)
    .settings(commonSettings: _*)
    .enablePlugins(GitVersioning)

lazy val chipper =
  project
    .in(file("chipper"))
    .settings(commonSettings: _*)
    .settings(name := "chipper")
    .enablePlugins(GitVersioning)

lazy val commonSettings = Seq(
  organization := "com.github.jw3",
  name := "example-xview",
  git.useGitDescribe := true,
  scalaVersion := "2.12.6",
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-Ywarn-unused-import",
    //"-Xfatal-warnings",
    "-Xlint:_",
    //
    // resolve apparent proguard collision
    "-Yresolve-term-conflict:object"
  ),
  libraryDependencies ++= {
    lazy val scalatestVersion = "3.0.3"

    Seq(
      "com.typesafe.akka" %% "akka-stream" % "2.5.17",
      "com.amazonaws" % "aws-java-sdk-s3" % "1.11.83",
      "com.github.jw3" %% "geotrellis-raster" % "12.2.0.0",
      "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "org.scalactic" %% "scalactic" % scalatestVersion % Test,
      "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    )
  }
)

lazy val dockerSettings = Seq(
  dockerBaseImage := sys.env.getOrElse("BASE_IMAGE", "openjdk:8"),
  dockerUpdateLatest := true
)
