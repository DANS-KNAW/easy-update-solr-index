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

import java.nio.charset.StandardCharsets

import com.yourmediashelf.fedora.client.request.FedoraRequest
import com.yourmediashelf.fedora.client.{ FedoraClient, FedoraClientException, FedoraCredentials }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils
import resource._

import scala.util.{ Failure, Try }

case class FedoraProviderImpl(credentials: FedoraCredentials) extends FedoraProvider with DebugEnhancedLogging {
  FedoraRequest.setDefaultClient(new FedoraClient(credentials))

  private def datastreamToString(pid: String, dsId: String): Try[String] = {
    managed(FedoraClient.getDatastreamDissemination(pid, dsId).execute())
      .flatMap(response => managed(response.getEntityInputStream))
      .map(inputStream => IOUtils.toString(inputStream, StandardCharsets.UTF_8))
      .tried
      .doIfSuccess(s => debug(s"Retrieved pid=$pid, ds=$dsId:$s"))
      .recoverWith {
        // unwrap the haystack around the needle
        case e: FedoraClientException if e.getStatus == 404 => Failure(new RuntimeException(
          // FedoraClientException gives the body of the HTTP-response as message.
          // This body is just a web page wrapped around the status line.
          // The FedoraClientException does not provide the status line of the HTTP-response,
          // so we re-assemble a simple message for probable common errors.
          s"Could not read datastream $dsId of $pid, HTTP code ${ e.getStatus }, $credentials"))
      }
  }

  override def getDc(pid: String): Try[String] = datastreamToString(pid, "DC")

  override def getRelsExt(pid: String): Try[String] = datastreamToString(pid, "RELS-EXT")

  override def getAmd(pid: String): Try[String] = datastreamToString(pid, "AMD")

  override def getPrsql(pid: String): Try[String] = datastreamToString(pid, "PRSQL")

  override def getEmd(pid: String): Try[String] = datastreamToString(pid, "EMD")
}
