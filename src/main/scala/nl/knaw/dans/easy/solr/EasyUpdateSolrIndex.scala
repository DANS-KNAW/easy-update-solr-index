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

import java.io.{File, FileInputStream}
import java.lang.Thread._

import com.yourmediashelf.fedora.client.FedoraClient._
import nl.knaw.dans.easy.solr.Defaults.filterDefaultOptions
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.IOUtils.readLines
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import scala.xml.PrettyPrinter

object EasyUpdateSolrIndex {

  private val log = LoggerFactory.getLogger(getClass)

  /** API for EasyIngestFlow, fails on any type of error */
  def run(implicit settings: Settings): Try[Unit] = Try {
      val dataset = settings.datasets.get.head
      settings.solr.update(createSolrDoc(dataset))
      log.info(s"Committed $dataset to SOLR index")
  }

  /** API for commandline, continues if some dataset has problems with some datastream */
  def main(args: Array[String]) = {

    val propsFile = new File(System.getProperty("app.home",""), "cfg/application.properties")
    val completedArgs = getDefaults(args, propsFile) ++ args
    implicit val settings = Settings(new Conf(completedArgs))
    log.info(s"$settings")
    if (settings.datasetQuery.isDefined)
      settings.datasetQuery.get.foreach(datasetsFromQuery(_))
    else if (settings.input.isDefined)
      readLines(new FileInputStream(settings.input.get)).asScala.foreach(execute)
    else if (settings.datasets.isDefined)
      settings.datasets.get.foreach(execute)
    else
      throw new IllegalArgumentException("No datasets specified to update")
  }

  def getDefaults(args: Array[String], propsFile: File): Seq[String] = {
    if (!propsFile.exists) {
      log.info(s"system property 'app.home' not set and/or could not find ${propsFile.getAbsolutePath}")
      Array[String]()
    }
    else {
      log.info(s"defaults from ${propsFile.getAbsolutePath}")
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
    objectsResponse.getPids.asScala.foreach(execute)
    if (objectsResponse.hasNext) datasetsFromQuery(query, Some(objectsResponse.getToken))
    else log.info(s"Finished $query")
  }

  def execute(dataset: String)
             (implicit settings: Settings): Unit = {
    Try {
      createSolrDoc(dataset)
    } match {
      case Failure(e) =>
        // exception not in log, to avoid tons of stack traces in case of input errors
        log.error(s"Fetching data for SOLR update of $dataset FAILED: ${e.getMessage}")
      case Success(solrDocString) => ()
        if (settings.output) println(solrDocString)
        if (log.isDebugEnabled) log.debug(s"Contents of SOLR document for $dataset: $solrDocString")
        log.info(s"Generated SOLR document for $dataset")
        if (settings.testMode) log.info(s"SOLR update skipped: $dataset")
        else settings.solr.update(solrDocString) match {
            case Success(_) => log.info(s"Committed $dataset to SOLR index")
            case Failure(e) => log.error(s"SOLR update FAILED: ${e.getMessage}", e)
        }
    }
    sleep(settings.timeout)
  }

  private def createSolrDoc(dataset: String)
                           (implicit settings: Settings): String = {
    new PrettyPrinter(160, 2).format(
      new SolrDocumentGenerator(settings.fedora, dataset).toXml
    )
  }
}
