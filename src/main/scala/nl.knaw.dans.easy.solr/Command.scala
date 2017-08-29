package nl.knaw.dans.easy.solr

import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets

import com.yourmediashelf.fedora.client.FedoraCredentials
import resource.Using

object Command extends App {

  val configuration = Configuration()
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
