package nl.knaw.dans.easy.solr

trait FedoraProvider {

  def getEmd(pid: String): String

  def getAmd(pid: String): String

  def getPrsql(pid: String): String



}
