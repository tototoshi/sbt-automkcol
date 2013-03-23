# WebDav Plugin 4 SBT

![Dev At CloudBees][1]

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

    resolvers += "DiversIT repo" at "http://repository-diversit.forge.cloudbees.com/release"

    addSbtPlugin("eu.diversit.sbt.plugin" % "webdav4sbt" % "1.1")

In your project's _build.sbt_
Add to the top of your project's _build.sbt_:

    import eu.diversit.sbt.plugin.WebDavPlugin._
and anywhere within the same file add:

    seq(WebDav.globalSettings : _*)
or

    seq(WebDav.scopedSettings : _*)

>_The plugin was build for both Scala 2.9.2 and 2.10.0 using SBT 0.12.2_

### Publishing Java artifacts with SBT

Version 1.1 added support for publishing plain Java artifacts. In contrast to Scala artifacts, Java artifacts do not need the Scala version in the artifact name.
To disable adding the Scala version in the artifact name, add this to your _build.sbt_:

    crossPath := false

Thanks to [jplikesbikes][5] for this contribution.

### Global or Scoped settings?

The plugin allows you to chose whether you would like the added tasks to be available in the global settings or in the scoped settings.
__Global settings__ means that the tasks added by this plugin are available in the global scope. So you can use it from sbt as

    >mkcol

The plugin adapts the _publish_ tasks, so the _mkcol_ task is executed beforehand. So running

    >publish
would always run both the _mkcol_ and _publish_ tasks.

__Scoped settings__ means that the tasks added by this plugin are available under the _webdav_ configuration.
To run the tasks defined by this plugin, you have to prepend the tasks with _webdav:_
In the sbt console, when you type _webdav:<TAB>_ , it will show you all tasks and settings available or used by this plugin.

In scoped settings, to run the _mkcol_ task, run

    >webdav:mkcol
or to publish to a WebDav location and create the directories, run

    >webdav:publish

With the scoped settings, when you run

    >publish
it will run the _normal_ publish task without the _mkcol_ addition.

## Publishing to WebDav

To publish to a WebDav location, like CloudBees, you need to add a _publishTo_ configuration in your project.
One way of doing that is to create a seperate _publish.sbt_ file in your project. The advantage is that you seperate the configuration for building your project
with the publish configuration in seperate files.

This is an example of a _publishTo_ configuration for CloudBees as is used by this project. It also shows how to create a Maven Pom for your project.

    credentials += (Some(new File("/private/diversit/.credentials/.credentials")) map(f => Credentials(f))).get

    publishTo <<= version { v: String =>
      val cloudbees = "https://repository-diversit.forge.cloudbees.com/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at cloudbees + "snapshot")
      else
        Some("releases" at cloudbees + "release")
    }

    publishMavenStyle := true

    publishArtifact in Test := false

    pomIncludeRepository := { x => false }

    pomExtra := (
      <url>https://bitbucket.org/diversit/webdav4sbt</url>
      <licenses>
        <license>
          <name>Eclipse Public License v1.0</name>
          <url>http://www.eclipse.org/legal/epl-v10.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>https://bitbucket.org/diversit/webdav4sbt</url>
        <connection>scm:git:https://bitbucket.org/diversit/webdav4sbt.git</connection>
      </scm>
      <developers>
        <developer>
          <id>diversit</id>
          <name>Joost den Boer</name>
          <url>http://www.diversit.eu</url>
        </developer>
      </developers>
    )


## WebDav

This project uses the library [Sardine][3]) as a Java [WebDav][4] client.

### MKCOL

Command to create collections (a.k.a directories) on a WebDav host.
The '[MKCOL][2]' command is currently the only WebDav command which is implemented by this plugin.

## Contributors

Thanks for their contribution:

*   [jplikesbikes][5] for crossPath support.

[1]: http://www.cloudbees.com/sites/default/files/Button-Built-on-CB-1.png
[2]: http://www.webdav.org/specs/rfc2518.html#METHOD_MKCOL
[3]: https://code.google.com/p/sardine/
[4]: http://www.webdav.org/specs/rfc2518.html
[5]: https://bitbucket.org/jplikesbikes
