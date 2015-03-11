import SonatypeKeys._

sonatypeSettings

sbtPlugin := true

organization := "com.github.tototoshi"

name := "sbt-automkcol"

version := "1.4.0"

libraryDependencies ++= Seq(
    "com.googlecode.sardine" % "sardine" % "146",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "com.typesafe"  % "config" % "1.0.0" % "test"
)

publishMavenStyle := true

publishTo <<= version { (v: String) => _publishTo(v) }

publishArtifact in Test := false

def _publishTo(v: String) = {
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/tototoshi/sbt-automkcol</url>
  <licenses>
    <license>
      <name>Eclipse Public License v1.0</name>
      <url>http://www.eclipse.org/legal/epl-v10.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:tototoshi/sbt-automkcol</url>
    <connection>scm:git:git@github.com:tototoshi/sbt-automkcol.git</connection>
  </scm>
  <developers>
    <developer>
      <id>diversit</id>
      <name>Joost den Boer</name>
      <url>http://www.diversit.eu</url>
    </developer>
    <developer>
      <id>tototoshi</id>
      <name>Toshiyuki Takahashi</name>
      <url>https://github.com/tototoshi</url>
    </developer>
  </developers>
)
