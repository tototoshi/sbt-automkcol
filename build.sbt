import de.johoop.jacoco4sbt._
import JacocoPlugin._
import eu.diversit.sbt.plugin.WebDavPlugin._

sbtPlugin := true

organization := "eu.diversit.sbt.plugin"

name := "webdav4sbt"

version := "1.1"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
    "com.googlecode.sardine" % "sardine" % "146",
    "org.scalatest" % "scalatest_2.9.2" % "2.0.M5b" % "test",
    "com.typesafe"  % "config" % "1.0.0" % "test"
)

crossScalaVersions := Seq("2.9.2", "2.10.0")

seq(WebDav.globalSettings : _*)

seq(jacoco.settings : _*)

