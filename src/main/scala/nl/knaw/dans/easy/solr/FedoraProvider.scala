package nl.knaw.dans.easy.solr

trait FedoraProvider {

  def getDc(pid: String): String

  def getEmd(pid: String): String

  def getAmd(pid: String): String

  def getPrsql(pid: String): String

  def getRelsExt(pid: String): String
}
