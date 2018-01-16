package com.github.tototoshi.sbt.automkcol

import sbt._
import sbt.std.TaskStreams

import scalaj.http.Http

object Plugin extends sbt.Plugin {

  trait AutoMkcolKeys {
    lazy val autoMkcol = config("autoMkcol")
    lazy val mkcol = TaskKey[Unit]("mkcol", "Make collections (folder) in remote WebDav location.")
  }

  trait MkCol {
    import StringPath._

    /**
     * Create artifact pathParts
     * -when is sbtPlugin then sbt version must be added to path
     * -when not crossPaths then not add any version number to path
     * -otherwise add scala version to path
     *
     * -when Scala 2.10.x then only add 2.10 to path
     * -otherwise add whole version to path (e.g. 2.9.2)
     */
    def createPaths(organization: String, artifactName: String, version: String, crossScalaVersions: Seq[String],
                    sbtVersion: String, crossPaths: Boolean, mavenStyle: Boolean, isSbtPlugin: Boolean) = {
      if(crossPaths){
        crossScalaVersions map { scalaVersion =>
          def topLevel(v: String, level: Int) = v split '.' take level mkString "."
          // The publish location for Scala 2.10.x is only '2.10', for Scala 2.9.x it is '2.9.x' !
          val scalaVer = if (scalaVersion.startsWith("2.9")) scalaVersion else topLevel(scalaVersion, 2)

          if (isSbtPlugin) {
            // e.g. /com/organization/artifact_2.9.2_0.12/0.1
            organization.asPath / s"${artifactName.toLowerCase}_${scalaVer}_${topLevel(sbtVersion,2)}" / version
          } else {
            // e.g. /com/organization/artifact_2.9.2/0.1
            organization.asPath / s"${artifactName.toLowerCase}_$scalaVer" / version
          }
        }
      }else{
        // e.g. /com/organization/artifact/0.1
        Seq( organization.asPath / artifactName.toLowerCase / version )
      }
    }
    /**
     * Return all collections (folder) for path.
     * @param path "/part/of/url"
     * @return List("part","part/of","part/of/url")
     */
    def pathCollections(path: String) = {
      def pathParts(path: String) = path.substring(1) split "/" toSeq
      def addPathToUrls(urls: List[String], path: String) = {
        if(urls.isEmpty) List(path)
        else urls :+ urls.last / path
      }

      pathParts(path).foldLeft(List.empty[String])(addPathToUrls)
    }


    /**
     * Get Maven root from Resolver. Returns None if Resolver is not MavenRepository.
     */
    def mavenRoot(resolver: Option[Resolver]) = resolver.collect { case m: MavenRepository => m.root }

    def publishToUrls(paths: Seq[String], resolver: Option[Resolver]) = resolver.collect {
      case m: MavenRepository =>
        paths map { path =>
          m.root / path
        }
    }

    /**
     * Check if url exists.
     */
    def exists(creds: DirectCredentials, url: String) = {
      def ensureTrailingSlash(p: String): String = if (p.endsWith("/")) { p } else { p + "/" }

      val response = Http(ensureTrailingSlash(url))
        .method("HEAD")
        .auth(creds.userName, creds.passwd)
        .timeout(30 * 1000, 30 * 1000)
        .asBytes
      response.code / 100 == 2
    }

    /**
     * Make collector (folder) for all paths.
     * @throws MkColException when urlRoot does not exist.
     */
    def mkcol(creds: DirectCredentials, urlRoot: String, paths: List[String], logger: Logger) =
      if(!exists(creds, urlRoot)) {
        throw new MkColException("Root '%s' does not exist." format urlRoot)
      } else {
        paths foreach { path =>
          val fullUrl = urlRoot / path
          if(!exists(creds, fullUrl)) {
            logger.info("automkcol: Creating collection '%s'" format fullUrl)
            Http(fullUrl)
              .method("MKCOL")
              .auth(creds.userName, creds.passwd)
              .timeout(30 * 1000, 30 * 1000)
              .asBytes
          }
        }
      }


    val hostRegex = """^http[s]?://([a-zA-Z0-9\.\-]*)/.*$""".r

    def getCredentialsForHost(publishTo: Option[Resolver], creds: Seq[Credentials]): DirectCredentials = {
      mavenRoot(publishTo).flatMap { root =>
        val hostRegex(host) = root
        Credentials.allDirect(creds).find { c =>
          c.host == host
        }
      }.getOrElse(sys.error("No credentials available to publish to WebDav"))
    }

    /**
     * Creates a collection for all artifacts that are going to be published
     * if the collection does not exist yet.
     */
    def mkcolAction(organization: String, artifactName: String, version: String, crossScalaVersions: Seq[String], sbtVersion: String,
                    crossPaths: Boolean, publishTo: Option[Resolver], credentialsSet: Seq[Credentials], streams: TaskStreams[_],
                    mavenStyle: Boolean, sbtPlugin: Boolean) = {
      streams.log.info("automkcol: Check whether (new) collection need to be created.")
      val artifactPaths = createPaths(organization, artifactName, version, crossScalaVersions, sbtVersion, crossPaths, mavenStyle, sbtPlugin)
      val artifactPathParts = artifactPaths map pathCollections

      def makeCollections(credentials: DirectCredentials): Unit = {
        mavenRoot(publishTo) foreach { root =>
          val creds = getCredentialsForHost(publishTo, credentialsSet)
          artifactPathParts foreach { pathParts =>
            mkcol(creds, root, pathParts, streams.log)
          }
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
      mkcol <<= (organization, name, version, crossScalaVersions, sbtVersion, crossPaths, publishTo, credentials, streams, publishMavenStyle, sbtPlugin) map mkcolAction,
      publish <<= publish.dependsOn(mkcol)
    )

    val scopedSettings = inConfig(autoMkcol)(globalSettings)
  }
}
