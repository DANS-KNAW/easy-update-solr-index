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

import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.JavaConverters._

class ConfSpec extends FlatSpec with Matchers {

  private val clo = new Conf(Array[String]()) {
    // avoids System.exit() in case of invalid arguments or "--help"
    override def verify(): Unit = {}
  }

  "distributed default properties" should "be valid options" in {
    val optKeys = clo.builder.opts.map(opt => opt.name).toArray
    val propKeys = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties")
      .getKeys.asScala.withFilter(key => key.startsWith("default."))

    propKeys.foreach(key => optKeys should contain(key.replace("default.", "")))
  }
}