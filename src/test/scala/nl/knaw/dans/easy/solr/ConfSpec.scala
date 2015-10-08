package nl.knaw.dans.easy.solr

import java.io.{ByteArrayOutputStream, File}
import java.lang.System.{clearProperty, setProperty}

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils.{readFileToString, write}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class ConfSpec extends FlatSpec with Matchers {

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

  "options in help info" should "be part of README.md" in {
    val readme = readFileToString(new File("README.md"))

    clearProperty("app.home")
    val options = printHelp().split("Options:")(1)

    if (!trimLines(readme).contains(trimLines(options)))
      fail(s"README.md should contain:\n$options")
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
    val readme = trimLines(readFileToString(new File("README.md")))

    clearProperty("app.home")
    val synopsis = printHelp().split("Defaults provided by")(0)
      .replaceFirst(".*","" /* first line is version line */).replaceFirst("Usage:","")

    for (line <- trimLines(synopsis).split("\n")) {
      if (! readme.contains(line.trim))
        fail(s"README.md should contain:\n$line")
    }
  }

  def trimLines(s: String): String =
    (for (line <- s.split("\n")) yield line.trim).mkString("\n")

  def sortedKeys(props: PropertiesConfiguration): Array[String] =
    props.getKeys.asScala.toArray.sortBy(x => x)

  def printHelp(): String = {
    val mockedOut = new ByteArrayOutputStream()
    Console.withOut(mockedOut) {
      new Conf(Array[String]()).printHelp()
    }
    mockedOut.toString
  }
}
