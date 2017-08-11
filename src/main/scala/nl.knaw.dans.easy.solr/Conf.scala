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

import java.net.URL

import org.rogach.scallop.{ ScallopConf, ScallopOption }

/**
 * Creates a set of parsed and validated command line arguments.
 *
 * @param args The default is a minimal list that does not exit due to invalid arguments.
 *             Though --version or --help would validate, Scallop will call System.exit(0).
 *             Otherwise the default option values make no sense.
 */
class Conf(args: Seq[String] = "-fhttp: -uu -pp -shttp: -b1 -t0 id".split(" ")) extends ScallopConf(args) {

  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-update-solr-index"
  val description = """Update EASY's SOLR Search Index with metadata of datasets in EASY's Fedora Commons Repository."""
  val synopsis = s"""$printedName [<option>...] [ <dataset-id> | <fcrepo-query> | <text-file> ] ..."""

  version(s"$printedName ${ Version() }")
  banner(
    s"""
       |  $description
       |
            |Usage:
       |
            |  $synopsis
       |
            |Options:
       |""".stripMargin)

  val fedora: ScallopOption[URL] = opt[URL]("fcrepo-server", required = true, short = 'f',
    descr = "URL of Fedora Commons Repository Server to connect to ")
  val user: ScallopOption[String] = opt[String]("fcrepo-user", required = true, short = 'u',
    descr = "User to connect to fcrepo-server")
  val password: ScallopOption[String] = opt[String]("fcrepo-password", required = true, short = 'p',
    descr = "Password for fcrepo-user")
  val solr: ScallopOption[URL] = opt[URL]("solr-update-url", required = true, short = 's',
    descr = "URL to POST SOLR documents to")
  val debug: ScallopOption[Boolean] = opt[Boolean]("debug", default = Some(false), short = 'd',
    descr = "If specified: only generate document(s), do not send anything to SOLR")
  val output: ScallopOption[Boolean] = opt[Boolean]("output", default = Some(false), short = 'o',
    descr = "If provided: output SOLR document(s) to stdout")
  val batchSize: ScallopOption[Int] = opt[Int]("dataset-batch-size", required = true, short = 'b',
    descr = "Number of datasets to update at once, maximized by fedora to 100 when selecting datasets with a query")
  val timeout: ScallopOption[Int] = opt[Int]("dataset-timeout", required = true, short = 't',
    descr = "Milliseconds to pause after processing a batch of datasets " +
      "to avoid reducing performance of the production system too much")

  val datasets: ScallopOption[List[String]] = trailArg[List[String]]("dataset-ids",
    descr = "One or more of: dataset id (for example 'easy-dataset:1'), " +
      "a file with a dataset id per line or " +
      "a fedora query that selects datasets (for example 'pid~easy-dataset:*', " +
      "see also help for 'specific fields' on <fcrepo-server>/objects) ")

  verify()
}
