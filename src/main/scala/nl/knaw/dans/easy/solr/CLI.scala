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

import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}
import scala.xml.PrettyPrinter

object CLI {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val conf = new Conf(args)
    val fedora = new FedoraProviderImpl(conf.fedora(), conf.user(), conf.password())
    val solrDocString = new PrettyPrinter(160, 2).format(new SolrDocumentGenerator(fedora, conf.dataset()).toXml)
    if(conf.output()) println(solrDocString)
    log.info(s"Generated SOLR document for ${conf.dataset()}")
    log.debug(s"Contents of SOLR document for ${conf.dataset()}: $solrDocString")
    if(!conf.debug()) {
      val solr = new SolrProviderImpl(conf.solr())
      solr.update(solrDocString) match {
        case Success(_) => log.info(s"Committed ${conf.dataset()} to SOLR index")
        case Failure(e) => log.error(s"SOLR update FAILED: ${e.getMessage}")
      }
    }
  }
}
