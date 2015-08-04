package nl.knaw.dans.easy.solr

import scala.xml._
import org.apache.commons.lang.StringUtils._

class SolrDocumentGenerator(fedora: FedoraProvider, pid: String) {
  val DC_NAMESPACE: String = "http://purl.org/dc/elements/1.1/"
  val EAS_NAMESPACE: String = "http://easy.dans.knaw.nl/easy/easymetadata/eas/"

  val dc = XML.loadString(fedora.getDc(pid))
  val emd = XML.loadString(fedora.getEmd(pid))
  val amd = XML.loadString(fedora.getAmd(pid))

  val fields = List(
    "dc_title" -> (emd \\ "title" \ "title").map(_.text),
    "dc_description" ->  (emd \\ "description" \ "description").map(_.text),
    "dc_creator" -> (emd \\ "creator" \ "creator").map(normalizeAuthor(_)),
    "dc_subject" -> (emd \\ "subject" \ "subject").map(_.text),
    "dc_publisher" -> (emd \\ "publisher" \ "publisher").map(_.text),
    "dc_contributor" -> (emd \\ "contributor" \ "contributor").map(normalizeAuthor(_)),
    "dc_date" -> (emd \\  "date" \ "date")


  def normalizeAuthor(creator: Node): String = {
    def surnameString = creator \\ "surname" match {
      case NodeSeq.Empty => ""
      case n => n.text
    }
    def titleInitialsPrefixOrganizationString =
      (List("title", "initials", "prefix")
      .map(creator \\ _)
      .map(_.text)
        :+ organizationString)
      .filter(isNotBlank(_))
      .mkString(" ")
    def organizationString = creator \\ "organization" match {
      case NodeSeq.Empty => ""
      case n if hasPersonalComponents => s"(${n.head.text})"
      case n => n.head.text
    }
    def hasPersonalComponents =
      List("title", "initials", "prefix", "DAI").flatMap(creator \\ _).exists( n => isNotBlank(n.text))

    if(creator.namespace == DC_NAMESPACE) creator.text
    else if(creator.namespace == EAS_NAMESPACE)
      List(surnameString, titleInitialsPrefixOrganizationString).filter(isNotBlank(_)).mkString(", ")
    else throw new RuntimeException(s"Invalid creator $creator")
  }

  def toXml: Elem =
    <add>
      <doc>
        <field name="repository_id">easy</field>
        <field name="sid">{pid}</field>
        {fields.map(f => createFieldsFor(f._1, f._2)).reduce(_ ++ _)}
      </doc>
    </add>

  def createFieldsFor(name: String, values: Seq[String]): NodeSeq =
    createField(name, values) ++ createSortField(name, values)

  def createField(name: String, values: Seq[String]): NodeSeq =
    values.map(value => <field name={name}>{value}</field>)

  def createSortField(name: String, values: Seq[String]): NodeSeq =
    if(values.isEmpty) NodeSeq.Empty
    else <field name={name + "_s"}>{values.mkString(" ")}</field>

}
