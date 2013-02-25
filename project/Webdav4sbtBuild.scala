import sbt._
import sbt.Keys._

object Webdav4sbtBuild extends Build {

  lazy val webdav4sbt = Project(
    id = "webdav4sbt",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "webdav4sbt",
      organization := "eu.diversit",
      version := "0.1",
      scalaVersion := "2.10.0"
      // add other settings here
    )
  )
}
