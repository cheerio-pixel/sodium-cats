import org.typelevel.sbt.tpolecat.*

ThisBuild / organization := "scala.sodium.cats"
ThisBuild / scalaVersion := "2.13.16"

// This disables fatal-warnings for local development. To enable it in CI set the `SBT_TPOLECAT_CI` environment variable in your pipeline.
// See https://github.com/typelevel/sbt-tpolecat/?tab=readme-ov-file#modes
ThisBuild / tpolecatDefaultOptionsMode := VerboseMode

lazy val root = (project in file(".")).settings(
  name := "sodium-cats",
  crossScalaVersions := Seq("2.13.16", "3.3.5"),
  libraryDependencies ++= Seq(
    "scala.sodium" %% "core" % "1.0.0",
    "org.typelevel" %% "cats-core" % "2.13.0",
  )
)
