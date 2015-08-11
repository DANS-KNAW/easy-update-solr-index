package nl.knaw.dans.easy.solr


import scala.xml._

class SolrDocumentGenerator(fedora: FedoraProvider, pid: String) {
  val DC_NAMESPACE: String = "http://purl.org/dc/elements/1.1/"
  val EAS_NAMESPACE: String = "http://easy.dans.knaw.nl/easy/easymetadata/eas/"
  val RDF_NAMESPACE: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

  val dc = XML.loadString(fedora.getDc(pid))
  val emd = XML.loadString(fedora.getEmd(pid))
  val amd = XML.loadString(fedora.getAmd(pid))
  val prsl = XML.loadString(fedora.getPrsql(pid))
  val relsExt = XML.loadString(fedora.getRelsExt(pid))

  val dcMappings = List("title", "description", "creator", "subject", "publisher",
    "contributor", "date", "type", "format", "identifier", "source", "language", "relation",
    "coverage", "rights")
    .map(s => (s"dc_${s}" -> (dc \ s).map(_.text)))

  val dcMappingsSort = List("title", "creator", "publisher", "contributor")
    .map(s => (s"dc_${s}_s" -> (dc \\ s).map(_.text)))

  val emdDateMappings = List("created", "available", "submitted", "published", "deleted")
    .map(s => (s"emd_date_${s}" -> (emd \ "date" \ s).map(_.text)))

  val emdFormattedDateMappings = List("created", "available")
    .map(s => (s"emd_date_${s}_formatted" -> (emd \ "date" \ s).map(n => IsoDate.format(n.text, getPrecision(n)))))

  def getPrecision(n: Node): String =
    n.attribute("format") match {
      case Some(ns) => if(ns.size > 0) ns(0).text else ""
      case None => ""
    }

  val otherMappings =
      List("amd_assignee_id" -> (amd \ "workflowData" \ "assigneeId").map(_.text),
        "amd_depositor_id" -> (amd \ "depositorId").map(_.text),
        "amd_workflow_progress" -> (amd \ "datasetState").map(_.text),
        "emd_audience" -> (emd \ "audience" \ "audience").map(_.text),
        "psl_permission_status" -> (prsl \ "sequences" \\ "sequence").map(formatPrslString),
        "archaeology_dc_subject" -> (emd \ "subject" \ "subject").filter(isArchaeologySubject).map(_.text),
        "archaeology_dcterms_temporal" -> (emd \ "coverage" \ "temporal").filter(isArchaeologyTemporal).map(_.text),
        "dai_creator" -> ((emd \\ "creator" \ "creator").filter(_.namespace == EAS_NAMESPACE) \ "entityId").filter(hasDaiScheme).map(_.text),
        "dai_contributor" -> ((emd \\ "contributor" \ "contributor").filter(_.namespace == EAS_NAMESPACE) \ "entityId").filter(hasDaiScheme).map(_.text),
        "easy_collections" -> ((relsExt \\ "Description" \ "isCollectionMember")
          .map(_.attribute(RDF_NAMESPACE, "resource") match {
          case Some(attr) => attr.text
          case _ => ""
        }).map(_.replace("info:fedora/", "")))
      )

  def hasDaiScheme(n: Node): Boolean =
    n.attribute("scheme") match {
      case Some(Seq(n)) => n.text == "DAI"
      case _ => false
    }

  def isArchaeologySubject(n: Node): Boolean =
    (n.attribute(EAS_NAMESPACE, "scheme"), n.attribute(EAS_NAMESPACE, "schemeId")) match {
      case (Some(Seq(scheme)), Some(Seq(schemeId))) => scheme.text == "ABR" && schemeId.text == "archaeology.dc.subject"
      case _ => false
    }

  def isArchaeologyTemporal(n: Node): Boolean =
    (n.attribute(EAS_NAMESPACE, "scheme"), n.attribute(EAS_NAMESPACE, "schemeId")) match {
      case (Some(Seq(scheme)), Some(Seq(schemeId))) => scheme.text == "ABR" && schemeId.text == "archaeology.dcterms.temporal"
      case _ => false
    }

  def formatPrslString(n: Node): String =
    List((n \ "requesterId").text, (n \ "state").text, (n \ "stateLastModified").text).mkString(" ")

  def toXml: Elem =
    <add>
      <doc>
        <field name="repository_id">easy</field>
        <field name="sid">{pid}</field>
        {dcMappings.map(f => createField(f._1, f._2)).reduce(_ ++ _)}
        {dcMappingsSort.map(f => createSortField(f._1, f._2)).reduce(_ ++ _)}
        {emdDateMappings.map(f => createField(f._1, f._2)).reduce(_ ++ _)}
        {otherMappings.map(f => createField(f._1, f._2)).reduce(_ ++ _)}
      </doc>
    </add>

  def createField(name: String, values: Seq[String]): NodeSeq =
    values.map(value => <field name={name}>{value}</field>)

  def createSortField(name: String, values: Seq[String]): NodeSeq =
    if(values.isEmpty) NodeSeq.Empty
    else <field name={name}>{values.mkString(" ")}</field>

}
