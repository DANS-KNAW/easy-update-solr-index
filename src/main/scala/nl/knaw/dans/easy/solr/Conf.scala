/*******************************************************************************
  * Copyright 2015 DANS - Data Archiving and Networked Services
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  ******************************************************************************/

package nl.knaw.dans.easy.solr

import java.io.File
import java.net.URL

import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.ScallopConf

class Conf(args: Seq[String]) extends ScallopConf(args) {
  val props = new PropertiesConfiguration(new File(homedir, "cfg/application.properties"))
  printedName = "easy-update-solr-index"
  version(s"$printedName ${Version()}")
  banner(s"""
            | Update the EASY SOLR Search Index with data from the EASY Fedora Commons Repository.
            |
            | Usage: $printedName
            |              [-f <fcrepo-server>] \\
            |              [-u <fcrepo-user> \\
            |               -p <fcrepo-password>] \\
            |               -s <solr-update-url> \\
            |               -d
            |               -o
            |               <dataset-pid>
            | Options:
            |""".stripMargin)
  val fedora = opt[URL]("fcrepo-server", default = Some(new URL(props.getString("default.fcrepo-server"))),
      descr = "Fedora Commons Repository Server to connect to ")
  val user = opt[String]("fcrepo-user", default = Some(props.getString("default.fcrepo-user")),
      descr = "User to connect ot fcrepo-server")
  val password = opt[String]("fcrepo-password", default = Some(props.getString("default.fcrepo-password")),
      descr = "Password for fcrepo-user")
  val solr = opt[URL]("solr-update-url", default = Some(new URL(props.getString("default.solr-update-url"))),
      descr="URL to POST SOLR documents to")
  val debug = opt[Boolean]("debug", default = Some(false), descr = "Only generate document, do not send it to SOLR")
  val output = opt[Boolean]("output", default = Some(false), descr = "Ouput SOLR document to stdout")
  val dataset = trailArg[String]("dataset-pid", required = true, descr = "Dataset from which to take (meta-)data " +
    "with which to update the SOLR index")
}