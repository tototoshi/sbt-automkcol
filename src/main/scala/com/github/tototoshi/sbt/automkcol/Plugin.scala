package com.github.tototoshi.sbt.automkcol

import sbt._
import sbt.std.TaskStreams

import scala.util.matching.Regex
import scalaj.http.Http

object AutoMkcolPlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin

  /**
    * Create artifact pathParts
    * -when is sbtPlugin then sbt version must be added to path
    * -when not crossPaths then not add any version number to path
    * -otherwise add scala version to path
    *
    * -when Scala 2.10.x then only add 2.10 to path
    * -otherwise add whole version to path (e.g. 2.9.2)
    */
  private def createPaths(organization: String,
                          artifactName: String,
                          version: String,
                          crossScalaVersions: Seq[String],
                          sbtVersion: String,
                          crossPaths: Boolean,
                          mavenStyle: Boolean,
                          isSbtPlugin: Boolean): Seq[String] = {

    val org = PathUtil.organizationPath(organization)

    if (crossPaths) {
      crossScalaVersions map { scalaVersion =>
        if (isSbtPlugin) {
          PathUtil.join(
            org,
            s"${artifactName.toLowerCase}_${PathUtil.scalaBinaryVersion(scalaVersion)}_${PathUtil.sbtBinaryVersion(sbtVersion)}",
            version)
        } else {
          PathUtil.join(
            org,
            s"${artifactName.toLowerCase}_${PathUtil.scalaBinaryVersion(scalaVersion)}",
            version
          )
        }
      }
    } else {
      Seq(org, artifactName.toLowerCase, version)
    }
  }

  /**
    * Make collector (folder) for all paths.
    */
  private def createDirectories(webDav: WebDav, urlRoot: String, paths: Seq[String], logger: Logger): Unit =
    if (!webDav.exists(urlRoot)) {
      sys.error("Root '%s' does not exist." format urlRoot)
    } else {
      paths foreach { path =>
        val fullUrl = PathUtil.join(urlRoot, path)
        if (!webDav.exists(fullUrl)) {
          logger.info("automkcol: Creating collection '%s'" format fullUrl)
          webDav.mkcol(fullUrl)
        }
      }
    }

  private def getMavenRepositoryRoot(resolver: Resolver): Option[String] = resolver match {
    case m: MavenRepository => Some(m.root)
    case _ => None
  }

  private def getCredentialsForHost(publishTo: Option[Resolver], credss: Seq[Credentials]): DirectCredentials = {

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
  private def mkcolAction(organization: String,
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
    val artifactPathParts = artifactPaths.map(PathUtil.pathCollections)

    def makeCollections(credentials: DirectCredentials): Unit = {
      for {
        p <- publishTo
        root <- getMavenRepositoryRoot(p)
        creds = getCredentialsForHost(publishTo, credentialsSet)
        webDav = new WebDav(creds)
        pp <- artifactPathParts
      } {
        createDirectories(webDav, root, pp, streams.log)
      }
    }

    makeCollections(getCredentialsForHost(publishTo, credentialsSet))

    streams.log.info("automkcol: Done.")
  }

  object autoImport {
    lazy val mkcol = TaskKey[Unit]("mkcol", "Make collections (folder) in remote WebDav location.")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = {
    import sbt.Keys._
    Seq(
      mkcol := mkcolAction(
        organization.value,
        name.value,
        version.value,
        crossScalaVersions.value,
        sbtVersion.value,
        crossPaths.value,
        publishTo.value,
        credentials.value,
        streams.value,
        publishMavenStyle.value,
        sbtPlugin.value
      ),
      publish <<= publish.dependsOn(mkcol)
    )
  }

}

object PathUtil {

  def join(path: String*): String = path.mkString("/")

  def organizationPath(organization: String): String =
    organization.replace('.', '/')

  def scalaBinaryVersion(v: String): String = if (v.startsWith("2.9")) v else v.split('.').take(2).mkString(".")

  def sbtBinaryVersion(v: String): String = v.split('.').take(2).mkString(".")

  def ensureTrailingSlash(p: String): String = if (p.endsWith("/")) { p } else { p + "/" }

  /**
    * Return all collections (folder) for path.
    * @param path "/part/of/url"
    * @return List("part","part/of","part/of/url")
    */
  def pathCollections(path: String): Seq[String] = {
    path.split("/").scanLeft(List.empty[String])(_ :+ _).tail.map(_.mkString("/"))
  }

}

class WebDav(creds: DirectCredentials) {

  def exists(url: String): Boolean = {
    val response = Http(PathUtil.ensureTrailingSlash(url))
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
