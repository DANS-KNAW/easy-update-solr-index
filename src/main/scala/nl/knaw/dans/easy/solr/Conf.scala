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

class Conf(args: Seq[String] = Array[String]()) extends ScallopConf(args) {

  printedName = "easy-update-solr-index"

  val propsFile = new File(System.getProperty("app.home"), "cfg/application.properties")
  val props = if (propsFile.exists) new PropertiesConfiguration(propsFile)
              else new PropertiesConfiguration()

  version(s"$printedName ${Version()}")
  banner(s"""
            | Update EASY's SOLR Search Index with metadata of datasets in EASY's Fedora Commons Repository.
            |
            | Usage:
            |    $printedName [<option>...] -q <fcrepo-query>...
            |    $printedName [<option>...] -d <dataset-id>...
            |    $printedName [<option>...] --file <text-file-with-dataset-id-per-line>
            |
            | Defaults provided by: ${if (propsFile.exists()) propsFile.getCanonicalPath else "-"}
            |
            | Options:
            |""".stripMargin)
  val fedora = opt[URL]("fcrepo-server", default = Some(new URL(props.getString(
        "default.fcrepo-server",
        "http://localhost:8080/fedora"))),
      descr = "URL of Fedora Commons Repository Server to connect to ")
  val user = opt[String]("fcrepo-user", short = 'u', default = Some(props.getString(
        "default.fcrepo-user",
        "")),
      descr = "User to connect to fcrepo-server")
  val password = opt[String]("fcrepo-password", short = 'p', default = Some(props.getString(
        "default.fcrepo-password",
        "")),
      descr = "Password for fcrepo-user")
  val solr = opt[URL]("solr-update-url", default = Some(new URL(props.getString(
        "default.solr-update-url",
        "http://localhost:8080/solr"))),
      descr="URL to POST SOLR documents to")
  val debug = opt[Boolean]("debug", short = 'd',
      default = Some(false),
      descr = "If specified: only generate document(s), do not send anything to SOLR")
  val output = opt[Boolean]("output",  short = 'o',
      default = Some(false),
      descr = "If provided: output SOLR document(s) to stdout")
  val batchSize = opt[Int]("dataset-batch-size",  short = 'b',default = Some(props.getInt(
    "default.dataset-batch-size",
    100)),
    descr = "Number of datasets to read at once from the dataset-query")
  val timeout = opt[Int]("dataset-timeout", short = 't', default = Some(props.getInt(
    "default.dataset-timeout",
    1000)),
    descr = "Milliseconds to pause after processing a dataset " +
      "to avoid reducing performance of the production system too much")
  val datasetQuery = opt[List[String]]("fcrepo-query", short = 'q',
    descr = "Fedora query that selects datasets, " +
      "query example: 'pid~easy-dataset:*'. " +
      "see also help for 'specific fields' on <fcrepo-server>/objects")
  val datasets = opt[List[String]]("dataset-id", short = 'i',
      descr = "ID of dataset to update, for eaxample: easy-dataset:1")
  val input = opt[File]("file",
    descr = "Text file with a dataset-id per line")
  mutuallyExclusive(datasetQuery, datasets, input)
  dependsOnAll(batchSize,List(datasetQuery))
}