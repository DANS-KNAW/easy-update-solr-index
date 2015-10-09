package nl.knaw.dans.easy.solr

import java.io.File

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.scalatest.{Matchers, FlatSpec}

class DefaultsSpec extends FlatSpec with Matchers {

  "minimal args plus defaults" should "parse" in {

    val args = "-q pid~easy-dataset:*".split(" ")
    val completedArgs = Defaults.filter(args, loadProps) ++ args

    new Conf(completedArgs).batchSize.apply() shouldBe 100
  }

  "command line values" should "have precedence over default values" in {

    val args = "-b3 -u u --dataset-timeout 6 -i easy-dataset:1".split(" ")
    val completedArgs = Defaults.filter(args, loadProps) ++ args

    val conf = new Conf(completedArgs)
    conf.batchSize.apply() shouldBe 3
    conf.timeout.apply() shouldBe 6
    conf.user.apply() shouldBe "u"
  }

  def loadProps: PropertiesConfiguration = {
    val fileContent =
      """default.fcrepo-server=http://localhost:8080/fedora
        |default.fcrepo-user=somebody
        |default.fcrepo-password=secret
        |default.solr-update-url=http://localhost:8080/solr
        |default.dataset-batch-size=100
        |default.dataset-timeout=1000
        | """.stripMargin
    val tmpFile = new File("target/test/application.properties")
    FileUtils.write(tmpFile, fileContent)
    new PropertiesConfiguration(tmpFile)
  }
}
