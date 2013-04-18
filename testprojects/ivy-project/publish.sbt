credentials += (Some(new File("/private/diversit/.credentials/.credentials")) map(f => Credentials(f))).get

publishTo <<= version { v: String =>
  val cloudbees = "https://repository-diversit.forge.cloudbees.com/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at cloudbees + "snapshot")
  else
    Some("releases" at cloudbees + "release")
}

publishMavenStyle := false

publishArtifact in Test := false

