package nl.knaw.dans.easy.solr

import java.io.{ByteArrayOutputStream, File}
import java.lang.System.{clearProperty, setProperty}

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils.{readFileToString, write}
import org.scalatest.{OneInstancePerTest, FlatSpec, Matchers}

import scala.collection.JavaConverters._

class ConfSpec extends FlatSpec with Matchers with OneInstancePerTest {
  clearProperty("app.home")

  "options in help info" should "be part of README.md" in {
    val readme = readFileToString(new File("README.md"))
    val options = printHelp().split("Options:")(1)
    trimLines(readme) should include(trimLines(options))
  }

  "options with a configurable default" should "be in distributed application.properties" in {
    val propKeys = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties").getKeys.asScala.toList
    new Conf().builder.opts
      .filter(_.default().isDefined)
      .filter(_.converter.argType.toString != "FLAG")
      .foreach(opt => propKeys should contain (s"default.${opt.name}"))
  }

  "synopsis in help info" should "be part of README.md" in {
    val readme = trimLines(readFileToString(new File("README.md")))
    val synopsis = printHelp().split("Defaults provided by")(0)
      .replaceFirst(".*","" /* first line is version line */).replaceFirst("Usage:","")
    trimLines(synopsis).split("\n").foreach(line => readme should include(line.trim))
  }

  def trimLines(s: String): String = s.split("\n").map(_.trim).mkString("\n")

  def printHelp(): String = {
    val mockedStdOut = new ByteArrayOutputStream()
    Console.withOut(mockedStdOut) {
      new Conf().printHelp()
    }
    mockedStdOut.toString
  }
}
