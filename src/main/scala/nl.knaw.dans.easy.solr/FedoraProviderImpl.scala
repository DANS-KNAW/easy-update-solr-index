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

import com.yourmediashelf.fedora.client.FedoraClient._
import com.yourmediashelf.fedora.client.request.FedoraRequest
import com.yourmediashelf.fedora.client.{ FedoraClient, FedoraClientException, FedoraCredentials }
import org.apache.commons.io.IOUtils
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.Try

case class FedoraProviderImpl(credentials: FedoraCredentials) extends FedoraProvider {
  val log: Logger = LoggerFactory.getLogger(getClass)
  FedoraRequest.setDefaultClient(
    new FedoraClient(
      credentials
    ) {override def toString = s"${ super.toString } ($credentials)" }
  )

  private def datastreamToString(pid: String, dsId: String): String = {
    val inputStream = Try {
      getDatastreamDissemination(pid, dsId).execute()
    }.recoverWith {
      // unwrap the haystack around the needle
      case e: FedoraClientException if e.getStatus == 404 => throw new RuntimeException(
        // FedoraClientException gives the body of the HTTP-response as message.
        // This body is just a web page wrapped around the status line.
        // The FedoraClientException does not provide the status line of the HTTP-response,
        // so we re-assemble a simple message for probable common errors.
        s"Could not read datastream $dsId of $pid, HTTP code ${ e.getStatus }, $credentials"
      )
    }.get.getEntityInputStream
    try {
      val s = IOUtils.toString(inputStream, "UTF-8")
      if (log.isDebugEnabled) log.debug(s"Retrieved pid=$pid, ds=$dsId:$s")
      s
    } finally {
      inputStream.close()
    }
  }

  override def getDc(pid: String): String = datastreamToString(pid, "DC")

  override def getRelsExt(pid: String): String = datastreamToString(pid, "RELS-EXT")

  override def getAmd(pid: String): String = datastreamToString(pid, "AMD")

  override def getPrsql(pid: String): String = datastreamToString(pid, "PRSQL")

  override def getEmd(pid: String): String = datastreamToString(pid, "EMD")
}
