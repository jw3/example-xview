lazy val `example-xview` =
  project
    .in(file("."))
    .aggregate(chipper, cluster)
    .settings(commonSettings: _*)
    .enablePlugins(GitVersioning)

lazy val chipper =
  project
    .in(file("chipper"))
    .settings(commonSettings: _*)
    .settings(name := "chip")
    .settings(buildinfoSettings: _*)
    .settings(debianSettings: _*)
    .enablePlugins(JavaServerAppPackaging, DebianPlugin, DockerPlugin, GitVersioning, BuildInfoPlugin)

lazy val cluster =
  project
    .in(file("cluster"))
    .dependsOn(chipper)
    .settings(commonSettings: _*)
    .settings(name := "cluster")
    .settings(buildinfoSettings: _*)
    .settings(debianSettings: _*)
    .settings(dockerSettings: _*)
    .enablePlugins(JavaServerAppPackaging, DebianPlugin, DockerPlugin, GitVersioning, BuildInfoPlugin)

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
    lazy val akkaVersion = "2.5.17"
    lazy val akkaHttpVersion = "10.1.5"
    lazy val scalatestVersion = "3.0.3"

    Seq(
      "com.iheart" %% "ficus" % "1.4.0",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
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

lazy val debianSettings = Seq(
  maintainer in Debian := "jw3",
  debianPackageDependencies := Seq("java8-runtime-headless")
)

lazy val buildinfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "com.github.jw3.xview",
  buildInfoUsePackageAsPath := true
)
