sbtPlugin := true

name := "webdav4sbt"

organization := "eu.diversit.sbt.plugin"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
    "com.googlecode.sardine" % "sardine" % "146",
    "org.scalatest" % "scalatest_2.9.2" % "2.0.M5b" % "test",
    "com.typesafe"  % "config" % "1.0.0" % "test"
)

crossScalaVersions := Seq("2.9.2", "2.10.0")