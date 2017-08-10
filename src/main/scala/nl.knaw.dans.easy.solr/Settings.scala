/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr

import java.net.URL

import com.yourmediashelf.fedora.client.FedoraCredentials

object Settings {
  def apply(conf: Conf): Settings = new Settings(
    batchSize = conf.batchSize.apply(),
    timeout = conf.timeout.apply(),
    testMode = conf.debug(),
    output = conf.output(),
    datasets = conf.datasets.apply(),
    solr = new SolrProviderImpl(conf.solr()),
    fedora = new FedoraProviderImpl(
      new FedoraCredentials(
        conf.fedora(),
        conf.user(),
        conf.password()
      ) {override def toString = s"FedoraCredentials (${conf.fedora()}, ${conf.user()}, ...)"}
    )
  )

  /** Backward compatible for EasyIngestFlow */
  def apply(fedoraCredentials: FedoraCredentials,
            dataset: String,
            solr: URL
             ):Settings = new Settings(
    testMode = false,
    datasets = List(dataset),
    solr = new SolrProviderImpl(solr),
    fedora = new FedoraProviderImpl(fedoraCredentials)
  )
}

case class Settings(batchSize: Int = 100,
                    timeout: Int = 1000,
                    testMode: Boolean = true,
                    output: Boolean = false,
                    datasets: List[String] = List(),
                    solr: SolrProvider,
                    fedora: FedoraProvider) {
}
