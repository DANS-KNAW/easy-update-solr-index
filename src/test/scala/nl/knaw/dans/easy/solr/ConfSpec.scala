package nl.knaw.dans.easy.solr

import java.io.ByteArrayOutputStream

import org.scalatest.{FlatSpec, Matchers}

import scala.runtime.BoxedUnit

class ConfSpec extends FlatSpec with Matchers {

  "printHelp" should "produce help info with values from custom properties file" in {

    val result = executeWithHomeDir("src/test/resources")

    // assert values that are neither hard coded nor in assembly/dist but in test/resources
    result should include("127.0.0.1")
    result should include("(default = secret)")
    result should include("(default = somebody)")
    result should include("src/test/resources")
    result should include("${project.version}") // FIXME ?
  }

  it should "be pasted into README.md" in {

    val result = executeWithHomeDir(null)
    println(result)

    // assert values that not from any application.properties file but hard coded defaults
    result should include("localhost")
    result should include("Password for fcrepo-user (default = )")
    result should include("User to connect to fcrepo-server (default = )")
    result should include("Defaults provided by: -")
  }

  def executeWithHomeDir(dir: String): String = {
    val conf = new Conf(Array[String]()) {
      override def getHomeDir = {
        dir
      }
    }
    val mockedOut = new ByteArrayOutputStream()
    Console.withOut(mockedOut) {
      conf.printHelp() shouldBe a[BoxedUnit]
    }
    mockedOut.toString
  }

  // Conf should have extended LazyScalloArg for the following tests
  // now the terminate not just the test but the complete test-suit

  ignore should "exit on batch-size without dataset-query" in {
    new Conf(Array[String]("-b1"))
  }
  ignore should "exit on batch-query with dataset-id" in {
    new Conf(Array[String]("-qX", "id"))
  }
}
