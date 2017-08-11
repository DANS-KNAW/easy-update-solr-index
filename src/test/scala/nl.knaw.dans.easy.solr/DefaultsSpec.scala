/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr

import java.io.File

import nl.knaw.dans.easy.solr.Defaults.filterDefaultOptions
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.scalatest.{ FlatSpec, Matchers }

class DefaultsSpec extends FlatSpec with Matchers {

  val props: PropertiesConfiguration = {
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

  private def TestConf(args: Array[String]) = {
    new Conf(args) {
      // avoids System.exit() in case of invalid arguments or "--help"
      override def verify(): Unit = {}
    }
  }

  "minimal command line" should "apply default values" in {

    val args = "easy-dataset:1".split(" ")
    val completedArgs = filterDefaultOptions(props, TestConf(args), args) ++ args

    val conf = new Conf(completedArgs)
    conf.batchSize() shouldBe 100
    conf.timeout() shouldBe 1000
    conf.user() shouldBe "somebody"
  }

  "command line values" should "have precedence over default values" in {

    val args = "-b3 -u u --dataset-timeout 6 easy-dataset:1".split(" ")
    val completedArgs = filterDefaultOptions(props, TestConf(args), args) ++ args

    val conf = new Conf(completedArgs)
    conf.batchSize() shouldBe 3
    conf.timeout() shouldBe 6
    conf.user() shouldBe "u"
  }
}
