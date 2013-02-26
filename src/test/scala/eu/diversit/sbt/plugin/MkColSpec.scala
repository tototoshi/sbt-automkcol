package eu.diversit.sbt.plugin

import org.scalatest.{OptionValues, FeatureSpec}
import org.scalatest.matchers.ShouldMatchers
import eu.diversit.sbt.plugin.WebDavPlugin.MkCol
import sbt.{DirectCredentials, Credentials, JavaNet1Repository, MavenRepository}
import sbt.std.Streams
import com.typesafe.config.ConfigFactory

/**
 * A sbt.Logger implementation for testing.
 */
trait TestLogger {

  val testLogger = new sbt.Logger {
    def trace(t: => Throwable) { println(t.getMessage) }

    def success(message: => String) { println(message) }

    import sbt.Level
    def log(level: Level.Value, message: => String) { println("%s: %s" format (level, message))}
  }
}

/**
 * Test mkcol action.
 */
class MkColSpec extends FeatureSpec with ShouldMatchers with OptionValues with TestLogger
        with MkCol with WebDavConfig {

  // Validate real values are set. If not, put a 'test.conf' file in classpath.
  username should not startWith("fill")

  import StringPath._

  feature("WebDav Make Collection") {
    scenario("Create artifact paths for all crossScalaVersions") {

      val paths = createPaths("com.organization", "name", "1.0.1", Seq("2.9.2", "2.10.0"), "0.12.2")
      paths should contain ("/com/organization/name_2.9.2_0.12/1.0.1")
      paths should contain ("/com/organization/name_2.10.0_0.12/1.0.1")
    }

    scenario("pathCollections should return all collections for path") {
      pathCollections("/com/organization/name_2.10.0_0.12/1.0.1") should equal(List("com", "com/organization", "com/organization/name_2.10.0_0.12", "com/organization/name_2.10.0_0.12/1.0.1"))
    }

    scenario("Update paths with 'publishTo' location") {
      val paths = Seq("/com/one", "/com/two")
      val resolver = Some(MavenRepository("releases", "http://some.url/"))

      val publishUrls = publishToUrls(paths, resolver)
      publishUrls should not be(None)
      publishUrls.get should contain ("http://some.url/com/one")
      publishUrls.get should contain ("http://some.url/com/two")
    }

    scenario("Exists should return true for existing urls") {
      import com.googlecode.sardine._

      val sardine = SardineFactory.begin(username, password)
      exists(sardine, webdavUrl / "de") should be(true)
      exists(sardine, webdavUrl / "non-existing-folder") should be(false)
    }

    scenario("Exists should return false for non existing urls (and not throw Exception)") {
      import com.googlecode.sardine._

      val sardine = SardineFactory.begin()
      exists(sardine, "https://fake.url/not-exist") should be(false)
    }

    scenario("Maven root should return root from MavenRepository") {
      val resolver = Some(MavenRepository("releases", "http://some.url/"))
      mavenRoot(resolver) should be(Some("http://some.url/"))
    }

    scenario("Maven root should return None if resolver not Maven Repo") {
      mavenRoot(Some(JavaNet1Repository)) should be(None)
    }

    scenario("Make collection should throw exception when urlRoot does not exist") {
      import com.googlecode.sardine._
      val sardine = SardineFactory.begin(username, password)

      intercept[MkColException] {
        mkcol(sardine, "https://fake.url/not-exist", List(), testLogger)
      }
    }

    scenario("getCredentialsForHost should return credentials for host") {
      import java.io.File
      val credentials = Seq(Credentials("realm", "host.name", "user", "pwd"),
        Credentials(new File(".")),
        Credentials("realm2", "host2.name", "user", "pwd"))

      val resolver = Some(MavenRepository("releases", "http://host2.name/"))

      val foundCredentials = getCredentialsForHost(resolver, credentials)
      foundCredentials should not be(None)
      foundCredentials map {
        case c:DirectCredentials => c.host should be("host2.name")
        case _ => fail("Wrong credentials found")
      }
    }

    scenario("Make collection (single level) should create only one directory") {
      import com.googlecode.sardine._
      val sardine = SardineFactory.begin(username, password)

      mkcol(sardine, webdavUrl, List("testing"), testLogger)
      exists(sardine, webdavUrl / "testing") should be(true)

      sardine.delete(webdavUrl / "testing/")
    }

    scenario("Make collection (multi level) should create only all directories") {
      import com.googlecode.sardine._
      val sardine = SardineFactory.begin(username, password)

      mkcol(sardine, webdavUrl, List("testing","testing/123","testing/123/456"), testLogger)
      exists(sardine, webdavUrl / "testing") should be(true)
      exists(sardine, webdavUrl / "testing/123") should be(true)
      exists(sardine, webdavUrl / "testing/123/456") should be(true)

      sardine.delete(webdavUrl / "testing/")
    }

    scenario("mkcolAction should create folders with all crossScalaVersions") {
      import java.io.File
      val streams = Streams[String]((_) => new File("target"), (_) => "Test", (_,_) => testLogger)("Test")
      val credentials = Seq(Credentials("realm", host, username, password))
      mkcolAction("test.org.case", "testcase", "1.0.1", Seq("2.9.2", "2.10.0"), "0.12.2", Some(MavenRepository("releases", webdavUrl)), credentials, streams)

      import com.googlecode.sardine._
      val sardine = SardineFactory.begin()
      exists(sardine, webdavUrl / "test/org/case/testcase_2.9.2_0.12/1.0.1") should be(true)
      exists(sardine, webdavUrl / "test/org/case/testcase_2.10.0_0.12/1.0.1") should be(true)

      val sardine2 = SardineFactory.begin(username, password)
      sardine2.delete(webdavUrl / "test/")
    }

    scenario("mkcolAction should throw exception when no credentials available") {
      import java.io.File
      val streams = Streams[String]((_) => new File("target"), (_) => "Test", (_,_) => testLogger)("Test")
      val credentials = Seq(Credentials("realm", "dummy.url", "user", "pwd"))

      intercept[MkColException] {
        mkcolAction("test.org.case", "testcase", "1.0.1", Seq("2.9.2", "2.10.0"), "0.12.2", Some(MavenRepository("releases", webdavUrl)), credentials, streams)
      }
    }
  }
}

/**
 * Load WebDav config from file
 */
trait WebDavConfig {

  private val dummyConfig = ConfigFactory.load("test-dummy")
  private val config = ConfigFactory.parseFile(new java.io.File("/private/diversit/test.conf")).withFallback(dummyConfig).getConfig("webdav")
  val username = config.getString("username")
  val password = config.getString("password")
  val webdavUrl = config.getString("url")
  val host = config.getString("host")
}
