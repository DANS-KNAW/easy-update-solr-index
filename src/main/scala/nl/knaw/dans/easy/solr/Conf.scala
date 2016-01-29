/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr

import java.io.File
import java.net.URL

import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.ScallopConf

import scala.collection.JavaConverters._

/**
 * Creates a set of parsed and validated command line arguments.
 *
 * @param args The default is a minimal list that does not exit due to invalid arguments.
 *             Though --version or --help would validate, Scallop will call System.exit(0).
 *             Otherwise the default option values make no sense.
 */
class Conf(args: Seq[String] = "-fhttp: -uu -pp -shttp: -b1 -t0 id".split(" ")
            ) extends ScallopConf(args) {

  printedName = "easy-update-solr-index"

  version(s"$printedName ${Version()}")
  banner(s"""
            | Update EASY's SOLR Search Index with metadata of datasets in EASY's Fedora Commons Repository.
            |
            | Usage:
            |    $printedName [<option>...] [ <dataset-id> | <fcrepo-query> | <text-file> ] ...
            |
            | Options:
            |""".stripMargin)

  val fedora = opt[URL]("fcrepo-server", required = true, short= 'f',
    descr = "URL of Fedora Commons Repository Server to connect to ")
  val user = opt[String]("fcrepo-user", required = true, short = 'u',
    descr = "User to connect to fcrepo-server")
  val password = opt[String]("fcrepo-password", required = true, short = 'p',
    descr = "Password for fcrepo-user")
  val solr = opt[URL]("solr-update-url", required = true, short ='s',
    descr="URL to POST SOLR documents to")
  val debug = opt[Boolean]("debug", default = Some(false), short = 'd',
    descr = "If specified: only generate document(s), do not send anything to SOLR")
  val output = opt[Boolean]("output", default = Some(false), short = 'o',
    descr = "If provided: output SOLR document(s) to stdout")
  val batchSize = opt[Int]("dataset-batch-size", required = true, short = 'b',
    descr = "Number of datasets to update at once, maximized by fedora to 100 when selecting datasets with a query")
  val timeout = opt[Int]("dataset-timeout", required = true, short = 't',
    descr = "Milliseconds to pause after processing a batch of datasets " +
      "to avoid reducing performance of the production system too much")

  val datasets = trailArg[List[String]]("dataset-ids",
    descr = "One or more of: dataset id (for example 'easy-dataset:1'), " +
      "a file with a dataset id per line or " +
      "a fedora query that selects datasets (for example 'pid~easy-dataset:*', " +
      "see also help for 'specific fields' on <fcrepo-server>/objects) ")
}
