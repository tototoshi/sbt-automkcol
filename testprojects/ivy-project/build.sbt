import eu.diversit.sbt.plugin.WebDavPlugin._

name := "ivy-project"

scalaVersion := "2.10.1"

organization := "com.testdomain"

version := "0.1"

libraryDependencies ++= Seq(
  "org.gnu.inet" % "libidn" % "1.15"
)

//crossScalaVersions := Seq("2.9.2", "2.10.0")

//crossPaths := false

//publishMavenStyle := false

//credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

seq(WebDav.globalSettings : _*)

//publishTo := Some("Server TEST Repo" at "https://ivy.sflanker.ru/sbt/")
