lazy val `example-xview` =
  project
    .in(file("."))
    .aggregate(common, chipper, cluster)
    .settings(commonSettings: _*)
    .enablePlugins(GitVersioning)

lazy val common =
  project
    .in(file("common"))
    .settings(commonSettings: _*)
    .settings(name := "common")
    .settings(buildinfoSettings: _*)
    .enablePlugins(GitVersioning, BuildInfoPlugin)

lazy val chipper =
  project
    .in(file("chipper"))
    .dependsOn(common)
    .settings(commonSettings: _*)
    .settings(name := "chip")
    .settings(debianSettings: _*)
    .settings(dockerSettings: _*)
    .enablePlugins(JavaServerAppPackaging, DebianPlugin, DockerPlugin, GitVersioning)

lazy val cluster =
  project
    .in(file("cluster"))
    .dependsOn(common)
    .settings(commonSettings: _*)
    .settings(name := "cluster")
    .settings(debianSettings: _*)
    .settings(dockerSettings: _*)
    .enablePlugins(JavaServerAppPackaging, DebianPlugin, DockerPlugin, GitVersioning)

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
  resolvers += Resolver.bintrayRepo("jw3", "maven"),
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
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "1.0-M1",
      "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "1.0-M1",
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
  packageName in Docker := "xview-cluster",
  dockerBaseImage := sys.env.getOrElse("BASE_IMAGE", "openjdk:8-jre-slim"),
  daemonGroup in Docker := "root",
  dockerEntrypoint := Seq(),
  dockerUpdateLatest := true,
  dockerEnvVars += "PATH" → "$PATH:/opt/docker/bin"
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
