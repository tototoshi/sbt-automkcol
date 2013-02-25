package eu.diversit.sbt.plugin

import sbt._
import com.googlecode.sardine.{SardineFactory, Sardine}
import std.TaskStreams

/**
 * Convenience class to be able to write a String with '/' in code.
 */
object StringPath {
  class StringPath(val path: String) {
    def / (part: String): String = path +
      (if(path.endsWith("/")) "" else "/") +
      (if(part.startsWith("/")) part.substring(1) else part)

    def asPath: String = "/" + path.replace('.','/')
  }
  implicit def string2StringPath(path: String) = new StringPath(path)
}

object WebDavPlugin extends Plugin {

  trait WebDavKeys {
    lazy val mkcol = TaskKey[Unit]("mkcol", "Make collections (folder) in remote WebDav location.")
  }

  trait MkCol {
    import StringPath._

    /**
     * Create artifact pathParts
     */
    def createPaths(organization: String, artifactName: String, version: String, crossScalaVersions: Seq[String], sbtVersion: String) = {
      crossScalaVersions map { scalaVersion =>
        organization.asPath / (("%s_%s_%s") format (artifactName, scalaVersion, sbtVersion)) / version
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
    def mavenRoot(resolver: Option[Resolver]) = resolver match {
      case Some(m: MavenRepository) => Some(m.root)
      case _ => None
    }

    def publishToUrls(paths: Seq[String], resolver: Option[Resolver]) =  resolver match {
      case Some(m: MavenRepository) => {
        Some(paths map { path =>
          m.root / path
        })
      }
      case _ => None
    }

    /**
     * Check if url exists.
     */
    def exists(sardine: Sardine, url: String) = {
      try {
        sardine.exists(url)
      } catch {
        case _: Throwable => false
      }
    }

    /**
     * Make collector (folder) for all paths.
     * @throws MkColException when urlRoot does not exist.
     */
    def mkcol(sardine: Sardine, urlRoot: String, paths: List[String], logger: Logger) =
      if(!exists(sardine, urlRoot)) {
        throw new MkColException("Root '%s' does not exist." format urlRoot)
      } else {
        paths foreach { path =>
          val fullUrl = urlRoot / path
          if(!exists(sardine, fullUrl)) {
            logger.info("WebDav: Creating collection '%s'" format fullUrl)
            sardine.createDirectory(fullUrl)
          }
        }
      }

    val hostRegex = """^http[s]?://([a-zA-Z0-9\.\-]*)/.*$""".r
    def getCredentialsForHost(publishTo: Option[Resolver], creds: Seq[Credentials]) = {
      mavenRoot(publishTo) flatMap { root =>
        val hostRegex(host) = root
        creds find {
          case c: DirectCredentials => c.host == host
          case _ => false
        }
      }
    }

    /**
     * Creates a collection for all artifacts that are going to be published
     * if the collection does not exist yet.
     */
    def mkcolAction(organization: String, artifactName: String, version: String, crossScalaVersions: Seq[String], sbtVersion: String, publishTo: Option[Resolver], credentialsSet: Seq[Credentials], streams: TaskStreams[_]) = {
      val artifactPaths = createPaths(organization, artifactName, version, crossScalaVersions, sbtVersion)
      val artifactPathParts = artifactPaths map pathCollections

      def makeCollections(credentials: DirectCredentials) = {
        mavenRoot(publishTo) foreach { root =>
          val sardine = SardineFactory.begin(credentials.userName, credentials.passwd)
          artifactPathParts foreach { pathParts =>
            mkcol(sardine, root, pathParts, streams.log)
          }
        }
      }

      val cc = getCredentialsForHost(publishTo, credentialsSet)
      cc match {
        case Some(creds: DirectCredentials) => makeCollections(creds)
        case _ => throw new MkColException("No credentials available to publish to WebDav")
      }
    }

    case class MkColException(msg: String) extends RuntimeException(msg)
  }

  object WebDav extends MkCol with WebDavKeys {
    import sbt.Keys._
    val settings = Seq(
//      mkCol <<= () map mkColAction
    )
  }
}
