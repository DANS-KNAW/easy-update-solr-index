/**
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

import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import com.yourmediashelf.fedora.client.FedoraCredentials
import resource.Using

object Command extends App {

  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val clo = new CommandLineOptions(args, configuration)
  implicit val settings: Settings = new Settings(
    batchSize = clo.batchSize(),
    timeout = clo.timeout(),
    testMode = clo.debug(),
    output = clo.output(),
    datasets = clo.datasets(),
    solr = SolrProviderImpl(new URL(configuration.properties.getString("default.solr-update-url"))),
    fedora = FedoraProviderImpl(
      new FedoraCredentials(
        configuration.properties.getString("default.fcrepo-server"),
        configuration.properties.getString("default.fcrepo-user"),
        configuration.properties.getString("default.fcrepo-password"))))

  val files = settings.datasets.filter(new File(_).exists())
  val queries = settings.datasets.filter(_ startsWith "pid~")
  val ids = settings.datasets.toSet -- files -- queries
  EasyUpdateSolrIndex.executeBatches(ids.toSeq)
  for (file <- files) {
    EasyUpdateSolrIndex.executeBatches(Using.fileLines(StandardCharsets.UTF_8)(new File(file)).toSeq)
  }
  for (query <- queries) {
    EasyUpdateSolrIndex.datasetsFromQuery(query)
  }
}
