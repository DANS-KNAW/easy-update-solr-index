package nl.knaw.dans.easy.solr

import java.io.{ByteArrayOutputStream, File}
import java.lang.System.{clearProperty, setProperty}
import java.net.MalformedURLException

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils.{readFileToString, write}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._
import scala.runtime.BoxedUnit

class ConfSpec extends FlatSpec with Matchers {

  // by default tests don't use any cfg/application.properties
  clearProperty("app.home")

  "options in help info" should "be part of README.md" in {
    // clearProperty("app.homedir") does not help a maven build so this test should come first
    val readme = readFileToString(new File("README.md"))
    val options = printHelp().split("Options:")(1)
    if (!trim(readme).contains(trim(options)))
      throw new RuntimeException(s"README.md should contain:\n$options")
  }

  it should "show all items in distributed application.properties" in {

    write(new File("target/test/cfg/application.properties"),
    """default.fcrepo-server=http://127.0.0.1:8080/fedora
      |default.fcrepo-user=somebody
      |default.fcrepo-password=secret
      |default.solr-update-url=http://127.0.0.1:8080/solr
      |default.dataset-batch-size=10
      |default.dataset-timeout=100
      | """.stripMargin)

    val testProps = new PropertiesConfiguration("target/test/cfg/application.properties")
    val distProps = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties")
    sortedKeys(distProps) should be (sortedKeys(testProps))

    setProperty("app.home", "target/test")
    val helpInfo = printHelp().replaceAll("\n", " ").replaceAll(" +"," ")

    for (key <- testProps.getKeys.asScala)
      helpInfo should include(s"(default = ${testProps.getString(key)})")

    helpInfo should include("target/test/cfg/application.properties") // "defaults provided by"
  }

  "version variable" should "rather be substituted" in {
    printHelp() should include("${project.version}")
    // FIXME
  }

  "options with a configurable default" should "be in distributed application.properties" in {
    val propKeys = new PropertiesConfiguration(
      "src/main/assembly/dist/cfg/application.properties"
    ).getKeys.asScala.mkString("\n")
    for { opt <- new Conf(Array[String]()).builder.opts}{
      opt.default() match {
        case Some(_) =>
          if (!(opt.converter.argType.toString() == "FLAG"))
            propKeys should include (s"default.${opt.name}")
          println(opt.converter)
        case None => ()
      }
    }
  }

  "synopsis in help info" should "be part of README.md" in {
    val readme = trim(readFileToString(new File("README.md")))
    val synopsis = printHelp().split("Defaults provided by")(0)
      .replaceFirst(".*","" /* first line is version line */).replaceFirst("Usage:","")
    for (line <- trim(synopsis).split("\n"))
      if (!readme.contains(line.trim))
        throw new RuntimeException(s"README.md should contain:\n$line")
  }

  "invalid configuration" should "rather not break help or report to fix the configuration" in {
    setProperty("app.home", "src/main/assembly/dist")
    the[MalformedURLException] thrownBy printHelp() should
      have message "no protocol: {{ easy_update_solr_index_fcrepo_server }}"
    // FIXME need mean and lean recovery for "Some(new URL(props.getString" and "Some(props.getInt"
    //    val default = Try(new URL(new PropertiesConfiguration().getString(
    //      "default.solr-update-url","http://localhost:8080/solr") )) match {
    //      case Success(url: URL) => Some(url)
    //      case Failure(e) => None
    //    }
    // http://qnalist.com/questions/5474194/time-to-add-option-try-conversions-to-standard-library
  }

  def trim(s: String): String =
    (for (line <- s.split("\n")) yield line.trim).mkString("\n")

  def sortedKeys(props: PropertiesConfiguration): Array[String] =
    props.getKeys.asScala.toArray.sortBy(x => x)

  def printHelp(): String = {
    val conf = new Conf(Array[String]())
    val mockedOut = new ByteArrayOutputStream()
    Console.withOut(mockedOut) {
      conf.printHelp() shouldBe a[BoxedUnit]
    }
    mockedOut.toString
  }
}
