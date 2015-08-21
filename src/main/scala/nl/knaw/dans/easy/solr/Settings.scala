package nl.knaw.dans.easy.solr

import java.net.URL

import com.yourmediashelf.fedora.client.FedoraCredentials

object Settings {
  def apply(conf: Conf): Settings =
    new Settings(
      fedoraCredentials = new FedoraCredentials(conf.fedora(), conf.user(), conf.password()),
      solr = conf.solr(),
      debug = conf.debug(),
      output = conf.output(),
      dataset = conf.dataset())
}

case class Settings(fedoraCredentials: FedoraCredentials,
                    solr: URL,
                    debug: Boolean = false,
                    output: Boolean = false,
                    dataset: String)
