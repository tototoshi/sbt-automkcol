sbtPlugin := true

organization := "eu.diversit.sbt.plugin"

name := "webdav4sbt"

version := "1.3"

libraryDependencies ++= Seq(
    "com.googlecode.sardine" % "sardine" % "146",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "com.typesafe"  % "config" % "1.0.0" % "test"
)

crossScalaVersions := Seq("2.9.2", "2.10.0")
