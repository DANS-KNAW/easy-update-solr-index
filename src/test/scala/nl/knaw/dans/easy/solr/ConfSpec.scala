package nl.knaw.dans.easy.solr

import java.io.{ByteArrayOutputStream, File}
import java.lang.System.clearProperty

import nl.knaw.dans.easy.solr.CustomMatchers._
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

import scala.collection.JavaConverters._

class ConfSpec extends FlatSpec with Matchers with OneInstancePerTest {

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

  "options with a configurable default" should "be in distributed application.properties" in {
    val propKeys = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties").getKeys.asScala.toList
    new Conf().builder.opts
      .filter(_.default().isDefined)
      .filter(_.converter.argType.toString != "FLAG")
      .foreach(opt => propKeys should contain (s"default.${opt.name}"))
  }

  "synopsis in help info" should "be part of README.md" in {
    val synopsis = helpInfo.split("Options:")(0).split("Defaults provided by:")(1)
    new File("README.md") should containTrimmed(synopsis)
  }

  "default properties" should "be valid options" in {
    val optKeys = new Conf().builder.opts.map(opt => opt.name).toArray
    val propKeys = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties")
      .getKeys.asScala.withFilter(key => key.startsWith("default.") )

    propKeys.foreach(key => optKeys should contain (key.replace("default.","")) )
  }
}