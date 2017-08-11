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

import java.io.{ File, FileInputStream }
import java.lang.Thread._

import com.yourmediashelf.fedora.client.FedoraClient._
import nl.knaw.dans.easy.solr.Defaults.filterDefaultOptions
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.IOUtils.readLines
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }
import scala.xml.PrettyPrinter

object EasyUpdateSolrIndex {

  private val log = LoggerFactory.getLogger(getClass)

  /** API for EasyIngestFlow, assumes a single dataset id, fails on any type of error */
  def run(implicit settings: Settings): Try[Unit] = Try {
    settings.solr.update("<add>" + createSolrDoc(settings.datasets.head) + "</add>")
    log.info(s"Committed ${ settings.datasets.head } to SOLR index")
  }

  /** API for commandline, continues with the next batch if some dataset causes problems */
  def main(args: Array[String]): Unit = {

    val propsFile = new File(System.getProperty("app.home", ""), "cfg/application.properties")
    val completedArgs = getDefaults(args, propsFile) ++ args
    implicit val settings = Settings(new Conf(completedArgs))

    val files = settings.datasets.filter(s => new File(s).exists())
    val queries = settings.datasets.filter(s => s.startsWith("pid~"))
    val ids = settings.datasets.toSet -- files -- queries
    executeBatches(ids.toSeq)
    files.foreach(s => executeBatches(readLines(new FileInputStream(new File(s))).asScala))
    queries.foreach(datasetsFromQuery(_))
  }

  @tailrec
  def executeBatches(lines: Seq[String])(implicit settings: Settings): Unit = {
    val (current, remainder) = lines.splitAt(settings.batchSize)
    execute(current)
    if (remainder.nonEmpty)
      executeBatches(remainder)
  }

  def getDefaults(args: Array[String], propsFile: File): Seq[String] = {
    if (!propsFile.exists) {
      log.info(s"system property 'app.home' not set and/or could not find ${ propsFile.getAbsolutePath }")
      Array[String]()
    }
    else {
      log.info(s"defaults from ${ propsFile.getAbsolutePath }")
      filterDefaultOptions(new PropertiesConfiguration(propsFile), new Conf(), args)
    }
  }

  @tailrec
  def datasetsFromQuery(query: String, token: Option[String] = None)
                       (implicit settings: Settings): Unit = {
    val objectsQuery = findObjects().maxResults(settings.batchSize).pid.query(query)
    val objectsResponse = token match {
      case None =>
        log.info(s"Start $query")
        objectsQuery.execute
      case Some(t) =>
        objectsQuery.sessionToken(t).execute
    }
    execute(objectsResponse.getPids.asScala)
    if (objectsResponse.hasNext) datasetsFromQuery(query, Some(objectsResponse.getToken))
    else log.info(s"Finished $query")
  }

  def execute(datasets: Seq[String])
             (implicit settings: Settings): Unit = {
    val docs = datasets.map(createSolrDoc).filter(_.nonEmpty)
    val s = docs.mkString("<add>", ",", "</add>")
    if (docs.nonEmpty) settings.solr.update(s) match {
      case Success(_) => log.info(s"Committed ${ docs.size } documents for ${ datasets.size } datasets to SOLR index")
      case Failure(e) => log.error(s"SOLR update FAILED: ${ e.getMessage }", e)
    }
    sleep(settings.timeout)
  }

  private def createSolrDoc(dataset: String)
                           (implicit settings: Settings): String =
    Try {
      val solrDocString = new PrettyPrinter(160, 2).format(
        new SolrDocumentGenerator(settings.fedora, dataset).toXml
      )
      if (settings.output) println(solrDocString)
      log.info(s"Generated SOLR document for $dataset")
      if (log.isDebugEnabled) log.debug(s"Contents of SOLR document for $dataset: $solrDocString")
      solrDocString
    } match {
      case Success(s) => s
      case Failure(e) =>
        // exception not in log, to avoid tons of stack traces in case of input errors
        log.error(s"Fetching data for SOLR update of $dataset FAILED: ${ e.getMessage }")
        ""
    }
}
