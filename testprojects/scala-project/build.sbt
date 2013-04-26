import eu.diversit.sbt.plugin.WebDavPlugin._

seq(WebDav.globalSettings : _*)

name := "scala-project"

organization := "test_project"

scalaVersion := "2.10.0"

version := "0.1"

crossScalaVersions := Seq("2.9.2", "2.10.0")
