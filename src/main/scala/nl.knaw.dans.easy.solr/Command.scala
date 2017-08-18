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
import java.lang.Thread.sleep

import com.yourmediashelf.fedora.client.FedoraClient.findObjects
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils.readLines
import resource._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.reflectiveCalls
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }
import scala.xml.PrettyPrinter

object Command extends App with DebugEnhancedLogging {
  type FeedBackMessage = String

  val configuration = Configuration()

  val commandLine: CommandLineOptions = new CommandLineOptions(args, configuration) {
    verify()
  }
  val app = new EasyUpdateSolrIndexApp(new ApplicationWiring(configuration))

  managed(app)
    .acquireAndGet(app => {
      for {
        _ <- app.init()
        msg <- run(app)
      } yield msg
    })
    .doIfSuccess(msg => println(s"OK: $msg"))
    .doIfFailure { case e => logger.error(e.getMessage, e) }
    .doIfFailure { case NonFatal(e) => println(s"FAILED: ${ e.getMessage }") }

  private def run(app: EasyUpdateSolrIndexApp): Try[FeedBackMessage] = {

    implicit val settings: Settings = Settings(commandLine)
    val files = settings.datasets.filter(s => new File(s).exists())
    val queries = settings.datasets.filter(s => s.startsWith("pid~"))
    val ids = settings.datasets.toSet -- files -- queries
    executeBatches(ids.toSeq)
    files.foreach(s => executeBatches(readLines(new FileInputStream(new File(s))).asScala))
    queries.foreach(datasetsFromQuery(_))
    Success(s"Finished $queries")
  }

  @tailrec
  def executeBatches(lines: Seq[String])(implicit settings: Settings): Unit = {
    val (current, remainder) = lines.splitAt(settings.batchSize)
    execute(current)
    if (remainder.nonEmpty)
      executeBatches(remainder)
  }

  @tailrec
  def datasetsFromQuery(query: String, token: Option[String] = None)
                       (implicit settings: Settings): Unit = {
    val objectsQuery = findObjects().maxResults(settings.batchSize).pid.query(query)
    val objectsResponse = token match {
      case None =>
        logger.info(s"Start $query")
        objectsQuery.execute
      case Some(t) =>
        objectsQuery.sessionToken(t).execute
    }
    execute(objectsResponse.getPids.asScala)
    if (objectsResponse.hasNext) datasetsFromQuery(query, Some(objectsResponse.getToken))
    else logger.info(s"Finished $query")
  }

  def execute(datasets: Seq[String])
             (implicit settings: Settings): Unit = {
    val docs = datasets.map(createSolrDoc).filter(_.nonEmpty)
    val s = docs.mkString("<add>", ",", "</add>")
    if (docs.nonEmpty) settings.solr.update(s) match {
      case Success(_) => logger.info(s"Committed ${ docs.size } documents for ${ datasets.size } datasets to SOLR index")
      case Failure(e) => logger.error(s"SOLR update FAILED: ${ e.getMessage }", e)
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
      logger.info(s"Generated SOLR document for $dataset")
      logger.debug(s"Contents of SOLR document for $dataset: $solrDocString")
      solrDocString
    } match {
      case Success(s) => s
      case Failure(e) =>
        // exception not in log, to avoid tons of stack traces in case of input errors
        logger.error(s"Fetching data for SOLR update of $dataset FAILED: ${ e.getMessage }")
        ""
    }
}
