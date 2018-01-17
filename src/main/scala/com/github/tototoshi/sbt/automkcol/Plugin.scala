package com.github.tototoshi.sbt.automkcol

import sbt.std.TaskStreams
import sbt.{Def, DirectCredentials, _}

import scala.util.matching.Regex
import scalaj.http.Http

object Plugin extends sbt.Plugin {

  trait AutoMkcolKeys {
    lazy val autoMkcol = config("autoMkcol")
    lazy val mkcol = TaskKey[Unit]("mkcol", "Make collections (folder) in remote WebDav location.")
  }

  trait MkCol {

    private def join(path: String*): String = path.mkString("/")

    /**
      * Create artifact pathParts
      * -when is sbtPlugin then sbt version must be added to path
      * -when not crossPaths then not add any version number to path
      * -otherwise add scala version to path
      *
      * -when Scala 2.10.x then only add 2.10 to path
      * -otherwise add whole version to path (e.g. 2.9.2)
      */
    def createPaths(organization: String,
                    artifactName: String,
                    version: String,
                    crossScalaVersions: Seq[String],
                    sbtVersion: String,
                    crossPaths: Boolean,
                    mavenStyle: Boolean,
                    isSbtPlugin: Boolean): Seq[String] = {

      def organizationPath(organization: String): String =
        organization.replace('.', '/')

      def scalaBinaryVersion(v: String) = if (v.startsWith("2.9")) v else v.split('.').take(2).mkString(".")

      def sbtBinaryVersion(v: String) = v.split('.').take(2).mkString(".")

      if (crossPaths) {
        crossScalaVersions map { scalaVersion =>
          if (isSbtPlugin) {
            join(
              organizationPath(organization),
              s"${artifactName.toLowerCase}_${scalaBinaryVersion(scalaVersion)}_${sbtBinaryVersion(sbtVersion)}",
              version)
          } else {
            join(
              organizationPath(organization),
              s"${artifactName.toLowerCase}_${scalaBinaryVersion(scalaVersion)}",
              version
            )
          }
        }
      } else {
        Seq(join(organizationPath(organization), artifactName.toLowerCase, version))
      }
    }

    /**
      * Return all collections (folder) for path.
      * @param path "/part/of/url"
      * @return List("part","part/of","part/of/url")
      */
    def pathCollections(path: String): Seq[String] = {
      path.split("/").scanLeft(List.empty[String])(_ :+ _).tail.map(_.mkString("/"))
    }

    /**
      * Make collector (folder) for all paths.
      * @throws MkColException when urlRoot does not exist.
      */
    def mkcol(webDav: WebDav, urlRoot: String, paths: Seq[String], logger: Logger): Unit =
      if (!webDav.exists(urlRoot)) {
        throw new MkColException("Root '%s' does not exist." format urlRoot)
      } else {
        paths foreach { path =>
          val fullUrl = join(urlRoot, path)
          if (!webDav.exists(fullUrl)) {
            logger.info("automkcol: Creating collection '%s'" format fullUrl)
            webDav.mkcol(fullUrl)
          }
        }
      }

    def getMavenRepositoryRoot(resolver: Resolver): Option[String] = resolver match {
      case m: MavenRepository => Some(m.root)
      case _ => None
    }

    def getCredentialsForHost(publishTo: Option[Resolver], credss: Seq[Credentials]): DirectCredentials = {

      def getHost(url: String): Option[String] = {
        val hostRegex: Regex = """^http[s]?://([a-zA-Z0-9\.\-]*)/.*$""".r
        url match {
          case hostRegex(host) => Some(host)
          case _ => None
        }
      }

      val creds = for {
        p <- publishTo
        r <- getMavenRepositoryRoot(p)
        h <- getHost(r)
        c <- Credentials.allDirect(credss).find { c =>
          c.host == h
        }
      } yield c

      creds.getOrElse(sys.error("No credentials available to publish to WebDav"))
    }

    /**
      * Creates a collection for all artifacts that are going to be published
      * if the collection does not exist yet.
      */
    def mkcolAction(organization: String,
                    artifactName: String,
                    version: String,
                    crossScalaVersions: Seq[String],
                    sbtVersion: String,
                    crossPaths: Boolean,
                    publishTo: Option[Resolver],
                    credentialsSet: Seq[Credentials],
                    streams: TaskStreams[_],
                    mavenStyle: Boolean,
                    sbtPlugin: Boolean): Unit = {
      streams.log.info("automkcol: Check whether (new) collection need to be created.")
      val artifactPaths = createPaths(
        organization,
        artifactName,
        version,
        crossScalaVersions,
        sbtVersion,
        crossPaths,
        mavenStyle,
        sbtPlugin)
      val artifactPathParts = artifactPaths.map(pathCollections)

      def makeCollections(credentials: DirectCredentials): Unit = {
        for {
          p <- publishTo
          root <- getMavenRepositoryRoot(p)
          creds = getCredentialsForHost(publishTo, credentialsSet)
          webDav = new WebDav(creds)
          pp <- artifactPathParts
        } {
          mkcol(webDav, root, pp, streams.log)
        }
      }

      makeCollections(getCredentialsForHost(publishTo, credentialsSet))

      streams.log.info("automkcol: Done.")
    }

    class MkColException(msg: String) extends RuntimeException(msg)
  }

  object AutoMkcol extends MkCol with AutoMkcolKeys {
    import sbt.Keys._
    val globalSettings = Seq(
      mkcol <<= (
        organization,
        name,
        version,
        crossScalaVersions,
        sbtVersion,
        crossPaths,
        publishTo,
        credentials,
        streams,
        publishMavenStyle,
        sbtPlugin) map mkcolAction,
      publish <<= publish.dependsOn(mkcol)
    )

    val scopedSettings: Seq[Def.Setting[_]] = inConfig(autoMkcol)(globalSettings)
  }

  class WebDav(creds: DirectCredentials) {

    def exists(url: String): Boolean = {

      def ensureTrailingSlash(p: String): String = if (p.endsWith("/")) { p } else { p + "/" }

      val response = Http(ensureTrailingSlash(url))
        .method("HEAD")
        .auth(creds.userName, creds.passwd)
        .timeout(30 * 1000, 30 * 1000)
        .asBytes
      response.code / 100 == 2
    }

    def mkcol(url: String): Unit = {
      Http(url)
        .method("MKCOL")
        .auth(creds.userName, creds.passwd)
        .timeout(30 * 1000, 30 * 1000)
        .asBytes
    }

  }

}
