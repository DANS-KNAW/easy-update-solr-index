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

import java.net.URL

import scala.util.Try
import scalaj.http.Http

class SolrProviderImpl(solrUrl: URL) extends SolrProvider {
  override def update(doc: String): Try[Unit] = Try {
    val result = Http(solrUrl.toString)
      .header("Content-Type", "application/xml")
      .param("commit", "true").postData(doc)
      .asString
    if(result.isError) throw new RuntimeException(s"SOLR Update failed: ${result.statusLine}, details: ${result.body}")
  }
}