package nl.knaw.dans.easy.solr

import org.scalamock.scalatest.MockFactory
import org.scalatest._

class SolrDocumentGeneratorSpec extends FlatSpec
    with Matchers
    with Inside
    with MockFactory
    with OneInstancePerTest {
  val fedora = mock[FedoraProvider]

  "minimal EMD" should "result in only sid field" in {
    fedora.getDc _ expects * anyNumberOfTimes() returning "<dc/>"
    fedora.getEmd _ expects * anyNumberOfTimes() returning "<easymetadata/>"
    fedora.getAmd _ expects * anyNumberOfTimes() returning "<amd />"
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml

    docRoot.label should be("add")
    (docRoot \ "doc" \ "field") should have length(2)
    (docRoot \ "doc" \ "field" \\ "@name")(1).text should be ("sid")
    (docRoot \ "doc" \ "field")(1).text should be ("test-pid:123")
  }

  "one EMD element" should "result in normal and sortable fields" in {
    fedora.getDc _ expects * anyNumberOfTimes() returning "<dc/>"
    fedora.getEmd _ expects * anyNumberOfTimes() returning
      <easymetadata>
        <emd:title>
          <dc:title>Some title</dc:title>
        </emd:title>
      </easymetadata>.toString()
    fedora.getAmd _ expects * anyNumberOfTimes() returning "<amd />"
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val fields = (docRoot \ "doc" \ "field").map(f => ((f \ "@name").text, f.text))

    fields should have length(4)
    fields should contain("sid" -> "test-pid:123")
    fields should contain("dc_title" -> "Some title")
    fields should contain("dc_title_s" -> "Some title")
  }

  "EMD elements with multiple values" should "result in concatenated values in sortable field" in {
    fedora.getEmd _ expects * anyNumberOfTimes() returning
      <easymetadata>
        <emd:title>
          <dc:title>Title 1</dc:title>
          <dc:title>Title 2</dc:title>
        </emd:title>
        <emd:description>
          <dc:description>My description</dc:description>
        </emd:description>
      </easymetadata>.toString()
    fedora.getAmd _ expects * anyNumberOfTimes() returning "<amd />"
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val sortTitle = (docRoot \ "doc" \\ "field").find(f => (f \ "@name").text == "dc_title_s")

    inside (sortTitle)  {
      case Some(t) => t.text should be("Title 1 Title 2")
    }
  }

  "dc:creator element" should "distinguished from eas:creator" in {
    fedora.getEmd _ expects * anyNumberOfTimes() returning
      <easymetadata
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:creator>
          <dc:creator>Unsplit creator</dc:creator>
          <eas:creator>
            <eas:title>Title</eas:title>
            <eas:initials>I</eas:initials>
            <eas:prefix>prefix</eas:prefix>
            <eas:surname>Surname</eas:surname>
            <eas:organization>Organization</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
          <eas:creator>
            <eas:organization>Organization</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
        </emd:creator>
      </easymetadata>.toString()
    fedora.getAmd _ expects * anyNumberOfTimes() returning "<amd />"
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml

    val dcCreators = (docRoot \ "doc" \\ "field").filter(f => (f \ "@name").text == "dc_creator")
    dcCreators should have length(3)
    dcCreators(0).text should be("Unsplit creator")
    dcCreators(1).text should be("Surname, Title I prefix (Organization)")
    dcCreators(2).text should be("Organization")
  }
}
