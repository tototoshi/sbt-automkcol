# Auto MKCOL Plugin 4 SBT

This project is a fork of [webdav4sbt](https://bitbucket.org/diversit/webdav4sbt).

## Goal

When publishing artifact to a WebDav location, by default, the _publish_ task will not create the required collections which will cause the _publish_ task to fail when the required collections do not exist.
You could create the directories manually by mounting the WebDav location locally or by using 'curl', but this is tedious. Especially in a CI environment, you want everything to run automatically.

So, the goal of this project was "to create a plugin for SBT which creates the required collections (folders) on a WebDav location, for example CloudBees, so that SBT can publish the artifact to the WebDav location."

## Tasks

This plugin adds the following tasks

* mkcol  -  Uses the WebDav MKCOL command to create all collections (folder) which are required to be able to publish the project artifacts to a WebDav location.
It will only create the collections which do not exist yet.

* publish  -  Adapts the default _publish_ task to automatically run _mkcol_ before publishing the artifacts.

## Usage

To use the plugin, add these lines to your _project/plugins.sbt_ or to the global _.sbt/plugins/build.sbt_:

    addSbtPlugin("com.github.tototoshi" % "sbt-automkcol" % "2.1.0")

In your project's _build.sbt_
Add to the top of your project's _build.sbt_:

    enablePlugins(AutoMkcolPlugin)

### Publishing Java artifacts with SBT

Version 1.1 added support for publishing plain Java artifacts. In contrast to Scala artifacts, Java artifacts do not need the Scala version in the artifact name.
To disable adding the Scala version in the artifact name, add this to your _build.sbt_:

    crossPath := false

Thanks to [jplikesbikes][5] for this contribution.

### Ivy

Since version 1.2, the WebDav plugin supports publishing Ivy artifacts.
To publish an Ivy artifact, in your _build.sbt_ set:

    publishMavenStyle := false

## Publishing to WebDav

To publish to a WebDav location, like CloudBees, you need to add a _publishTo_ configuration in your project.
One way of doing that is to create a seperate _publish.sbt_ file in your project. The advantage is that you seperate the configuration for building your project
with the publish configuration in seperate files.

This is an example of a _publishTo_ configuration. It also shows how to create a Maven Pom for your project.

    credentials += (Some(new File("/private/tototoshi/.credentials/.credentials")) map(f => Credentials(f))).get

    publishTo <<= version { v: String =>
      val mymvnrepo = "https://mymvnrepo/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at mymvnrepo + "snapshot")
      else
        Some("releases" at mymvnrepo + "release")
    }

    publishMavenStyle := true

    publishArtifact in Test := false

    pomIncludeRepository := { x => false }

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
        <url>https://github.com/tototoshi/sbt-automkcol</url>
        <connection>scm:git:https://github.com/tototoshi/sbt-automkcol.git</connection>
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
          <url>http://github.com/tototoshi</url>
        </developer>
      </developers>
    )


### MKCOL

Command to create collections (a.k.a directories) on a WebDav host.
The '[MKCOL][2]' command is currently the only WebDav command which is implemented by this plugin.

## Versions

*   2.1.0 Support sbt 1.x
*   2.0.0 Rewrote using AutoPlugin
*   1.5   Rename com.github.tototoshi.sbt.automkcol.WebDavPlugin To com.github.tototoshi.sbt.automkcol.Plugin
*   1.4   Fork and rename to sbt-automkcol
*   1.3   (current) Fixed issue that sbtVersion should only be added for sbtPlugin projects
*   1.2   Added support for Ivy artifacts.
*   1.1   Added support for (Java) artifacts without Scala version.
*   1.0   First plugin release

## Contributors

Thanks for their contribution:

*   [jplikesbikes][5] for crossPath support.
*   Flanker_9 for raising the Ivy issue.

[1]: http://www.cloudbees.com/sites/default/files/Button-Built-on-CB-1.png
[2]: http://www.webdav.org/specs/rfc2518.html#METHOD_MKCOL
[3]: http://www.webdav.org/specs/rfc2518.html
[4]: https://bitbucket.org/jplikesbikes
