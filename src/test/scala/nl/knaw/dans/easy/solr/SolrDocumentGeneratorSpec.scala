package nl.knaw.dans.easy.solr

import org.scalamock.scalatest.MockFactory
import org.scalatest._

class SolrDocumentGeneratorSpec extends FlatSpec
    with Matchers
    with Inside
    with MockFactory
    with OneInstancePerTest {
  val fedora = mock[FedoraProvider]

  "minimal DC" should "result in only sid field" in {
    fedora.getDc _ expects * anyNumberOfTimes() returning "<dc/>"
    fedora.getEmd _ expects * anyNumberOfTimes() returning "<easymetadata/>"
    fedora.getAmd _ expects * anyNumberOfTimes() returning "<amd />"
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml

    docRoot.label should be("add")
    (docRoot \ "doc" \ "field") should have length(2)
    (docRoot \ "doc" \ "field" \\ "@name")(1).text should be ("sid")
    (docRoot \ "doc" \ "field")(1).text should be ("test-pid:123")
  }

  "one DC element" should "result in normal and sortable fields" in {
    fedora.getDc _ expects * anyNumberOfTimes() returning
      <dc>
        <title>Some title</title>
      </dc>.toString()
    fedora.getEmd _ expects * anyNumberOfTimes() returning "<easymetadata />"
    fedora.getAmd _ expects * anyNumberOfTimes() returning "<amd />"
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val fields = (docRoot \ "doc" \ "field").map(f => ((f \ "@name").text, f.text))

    fields should have length(4)
    fields should contain("sid" -> "test-pid:123")
    fields should contain("dc_title" -> "Some title")
    fields should contain("dc_title_s" -> "Some title")
  }

  "EMD elements with multiple values" should "result in concatenated values in sortable field" in {

    fedora.getDc _ expects * anyNumberOfTimes() returning
      <dc>
        <dc:title>Title 1</dc:title>
        <dc:title>Title 2</dc:title>
        <dc:description>My description</dc:description>
      </dc>.toString()
    fedora.getEmd _ expects * anyNumberOfTimes() returning "<easymetadata />"
    fedora.getAmd _ expects * anyNumberOfTimes() returning "<amd />"
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val sortTitle = (docRoot \ "doc" \\ "field").find(f => (f \ "@name").text == "dc_title_s")

    inside (sortTitle)  {
      case Some(t) => t.text should be("Title 1 Title 2")
    }
  }
}

