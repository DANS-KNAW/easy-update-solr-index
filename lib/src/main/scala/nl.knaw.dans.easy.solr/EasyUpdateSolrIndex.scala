/*
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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

import com.yourmediashelf.fedora.client.FedoraClient
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{ Failure, Try }
import scala.xml.PrettyPrinter

object EasyUpdateSolrIndex extends DebugEnhancedLogging {

  /** API for EasyIngestFlow, assumes a single dataset id, fails on any type of error */
  def run(implicit settings: Settings): Try[Unit] = Try {
    settings.datasets match {
      case Seq(dataset) =>
        settings.solr.update(s"<add>${ createSolrDoc(dataset) }</add>")
          .doIfSuccess(_ => logger.info(s"Committed $dataset to SOLR index"))
      case ds => Failure(new IllegalArgumentException(s"Exactly one dataset expected. Actually given: ${ ds.mkString("[", ", ", "]") }"))
    }
  }

  @tailrec
  def executeBatches(lines: Seq[String])(implicit settings: Settings): Unit = {
    val (current, remainder) = lines.splitAt(settings.batchSize)
    execute(current)
    if (remainder.nonEmpty)
      executeBatches(remainder)
  }

  @tailrec
  def datasetsFromQuery(query: String, token: Option[String] = None)(implicit settings: Settings): Unit = {
    val objectsQuery = FedoraClient.findObjects().maxResults(settings.batchSize).pid.query(query)

    val findObjects = token.map(objectsQuery.sessionToken)
      .getOrElse {
        logger.info(s"Start $query without session token")
        objectsQuery
      }

    managed(findObjects.execute())
      .acquireAndGet(objectsResponse => {
        execute(objectsResponse.getPids.asScala)
        if (objectsResponse.hasNext) Option(objectsResponse.getToken)
        else Option.empty
      }) match {
      case nextToken @ Some(_) => datasetsFromQuery(query, nextToken)
      case None => logger.info(s"Finished $query")
    }
  }

  def execute(datasets: Seq[String])(implicit settings: Settings): Unit = {
    val docs = datasets.map(createSolrDoc).filter(_.nonEmpty)

    if (docs.nonEmpty)
      settings.solr.update(docs.mkString("<add>", ",", "</add>"))
        .doIfSuccess(_ => logger.info(s"Committed ${ docs.size } documents for ${ datasets.size } datasets to SOLR index"))
        .doIfFailure { case e => logger.error(s"SOLR update FAILED: ${ e.getMessage }", e) }

    Thread.sleep(settings.timeout)
  }

  private def createSolrDoc(dataset: String)(implicit settings: Settings): String = {
    SolrDocumentGenerator(settings.fedora, dataset)
      .map(_.toXml)
      .map(new PrettyPrinter(160, 2).format(_))
      .doIfSuccess(solrDocString => {
        if (settings.output) println(solrDocString)
        logger.info(s"Generated SOLR document for $dataset")
        debug(s"Contents of SOLR document for $dataset: $solrDocString")
      })
      .doIfFailure {
        case e => logger.error(s"Fetching data for SOLR update of $dataset FAILED: ${ e.getMessage }", e)
      }
      .getOrElse("")
  }
}
