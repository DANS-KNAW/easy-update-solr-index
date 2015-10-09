package nl.knaw.dans.easy.solr

import java.io.{ByteArrayOutputStream, File}
import java.lang.System.clearProperty

import nl.knaw.dans.easy.solr.CustomMatchers._
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

import scala.collection.JavaConverters._

class ConfSpec extends FlatSpec with Matchers {

  clearProperty("app.home")
  def helpInfo = {
    val mockedStdOut = new ByteArrayOutputStream()
    Console.withOut(mockedStdOut) {
      new Conf().printHelp()
    }
    mockedStdOut.toString
  }

  "options in help info" should "be part of README.md" in {
    val options = helpInfo.split("Options:")(1)
    new File("README.md") should containTrimmed(options)
  }

  "synopsis in help info" should "be part of README.md" in {
    val synopsis = helpInfo.split("Options:")(0).split("Usage:")(1)
    new File("README.md") should containTrimmed(synopsis)
  }

  "first banner line" should "be part of README.md" in {
    val synopsis = helpInfo.split("\n")(1)
    new File("README.md") should containTrimmed(synopsis)
  }

  "distributed default properties" should "be valid options" in {
    val optKeys = new Conf().builder.opts.map(opt => opt.name).toArray
    val propKeys = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties")
      .getKeys.asScala.withFilter(key => key.startsWith("default.") )

    propKeys.foreach(key => optKeys should contain (key.replace("default.","")) )
  }
}