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

import com.yourmediashelf.fedora.client.request.FedoraRequest
import com.yourmediashelf.fedora.client.{FedoraClient, FedoraCredentials}
import com.yourmediashelf.fedora.client.FedoraClient._
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory

class FedoraProviderImpl(server: URL, user: String, password: String) extends FedoraProvider {
  val log = LoggerFactory.getLogger(getClass)

  val creds = new FedoraCredentials(server, user, password)
  val client = new FedoraClient(creds)
  FedoraRequest.setDefaultClient(client)

  private def datastreamToString(pid: String, dsId: String): String = {
    val s = IOUtils.toString(getDatastreamDissemination(pid, dsId).execute().getEntityInputStream)
    if(log.isDebugEnabled) log.debug(s"Retrieved pid=$pid, ds=$dsId:$s")
    s
  }

  override def getDc(pid: String): String = datastreamToString(pid, "DC")

  override def getRelsExt(pid: String): String = datastreamToString(pid, "RELS-EXT")

  override def getAmd(pid: String): String = datastreamToString(pid, "AMD")

  override def getPrsql(pid: String): String = datastreamToString(pid, "PRSQL")

  override def getEmd(pid: String): String = datastreamToString(pid, "EMD")
}
