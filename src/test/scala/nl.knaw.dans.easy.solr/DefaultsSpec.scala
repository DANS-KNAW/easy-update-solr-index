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

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.JavaConverters._

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
    new CommandLineOptions(args) {
      // avoids System.exit() in case of invalid arguments or "--help"
      override def verify(): Unit = {}
    }
  }

  "distributed default properties" should "be valid options" in {
    val clo = new CommandLineOptions(Array[String]()) {
      // avoids System.exit() in case of invalid arguments or "--help"
      override def verify(): Unit = {}
    }
    val optKeys = clo.builder.opts.map(opt => opt.name).toArray
    val propKeys = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties")
      .getKeys.asScala.withFilter(key => key.startsWith("default."))

    propKeys.foreach(key => optKeys should contain(key.replace("default.", "")))
  }

  "minimal command line" should "apply default values" in {

    val args = "easy-dataset:1".split(" ")

    val conf = new CommandLineOptions(args)
    conf.verify()
    conf.batchSize() shouldBe 100
    conf.timeout() shouldBe 1000
    conf.user() shouldBe "fedoraAdmin"
  }

  "command line values" should "have precedence over default values" in {

    val args = "-b3 -u u --dataset-timeout 6 easy-dataset:1".split(" ")

    val conf = new CommandLineOptions(args)
    conf.verify()
    conf.batchSize() shouldBe 3
    conf.timeout() shouldBe 6
    conf.user() shouldBe "u"
  }
}
