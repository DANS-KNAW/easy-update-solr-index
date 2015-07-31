package nl.knaw.dans.easy.solr

import scala.xml._

class SolrDocumentGenerator(fedora: FedoraProvider, pid: String) {
  val emd = XML.loadString(fedora.getEmd(pid))
  val amd = XML.loadString(fedora.getAmd(pid))

  val fields = List(
    "dc_title" -> emd \\ "title" \ "title",
    "dc_description" ->  emd \\ "description" \ "description"
  )

  def toXml: Elem =
    <add>
      <doc>
        <field name="sid">{pid}</field>
        {fields.map(f => createFieldsFor(f._1, f._2)).reduce(_ ++ _)}
      </doc>
    </add>

  def createFieldsFor(name: String, nodes: NodeSeq): NodeSeq =
    createField(name, nodes) ++ createSortableField(name, nodes)

  def createField(name: String, nodes: NodeSeq): NodeSeq =
    nodes.map(node => <field name={name}>{node.text}</field>)

  def createSortableField(name: String, nodes: NodeSeq): NodeSeq =
    if(nodes.isEmpty) NodeSeq.Empty
    else <field name={name + "_s"}>{nodes.map(_.text).mkString(" ")}</field>

}
