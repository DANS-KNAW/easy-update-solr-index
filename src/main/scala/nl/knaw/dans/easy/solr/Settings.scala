package nl.knaw.dans.easy.solr

import java.io.File
import java.net.URL

import com.yourmediashelf.fedora.client.FedoraCredentials

object Settings {
  def apply(conf: Conf): Settings = new Settings(
    batchSize = conf.batchSize.apply(),
    timeout = conf.timeout.apply(),
    testMode = !conf.applyUpdates(),
    output = conf.output(),
    input = conf.input.get,
    datasetQuery = conf.datasetQuery.get,
    datasets = conf.datasets.get,
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
    datasets = Some(List(dataset)),
    solr = new SolrProviderImpl(solr),
    fedora = new FedoraProviderImpl(fedoraCredentials)
  )
}

case class Settings(batchSize: Int = 100,
                    timeout: Int = 1000,
                    testMode: Boolean = true,
                    output: Boolean = false,
                    datasetQuery: Option[List[String]] = None,
                    datasets: Option[List[String]] = None,
                    input:Option[File] = None,
                    solr: SolrProvider,
                    fedora: FedoraProvider) {
}
