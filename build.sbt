lazy val `example-xview` =
  project
    .in(file("."))
    .aggregate(app)
    .settings(commonSettings: _*)
    .enablePlugins(GitVersioning)

lazy val app =
  project
    .in(file("core"))
    .settings(commonSettings: _*)
    .settings(name := "example-xview-app")
    .enablePlugins(GitVersioning)

lazy val commonSettings = Seq(
  organization := "com.github.jw3",
  name := "example-xview",
  git.useGitDescribe := true,
  scalaVersion := "2.12.6",
  libraryDependencies ++= {
    lazy val scalatestVersion = "3.0.3"

    Seq(
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
