package nl.knaw.dans.easy.solr

import org.scalamock.scalatest.MockFactory
import org.scalatest._
import scala.xml._

class SolrDocumentGeneratorSpec extends FlatSpec
    with Matchers
    with Inside
    with MockFactory
    with OneInstancePerTest {
  /*
   * Mocking and helper functions.
   */
  val fedora = mock[FedoraProvider]

  private def expectEmptyXmlByDefault = {
    expectDc(<dc/>)
    expectEmd(<easymetadata/>)
    expectAmd(<amd />)
    expectPrsl(<permissionSequenceList />)
    expectRelsExt(<RDF />)
  }

  private def expectDc(xml: Elem) = fedora.getDc _ expects * anyNumberOfTimes() returning(xml.toString)
  private def expectEmd(xml: Elem) = fedora.getEmd _ expects * anyNumberOfTimes() returning(xml.toString)
  private def expectAmd(xml: Elem) = fedora.getAmd _ expects * anyNumberOfTimes() returning(xml.toString)
  private def expectPrsl(xml: Elem) = fedora.getPrsql _ expects * anyNumberOfTimes() returning(xml.toString)
  private def expectRelsExt(xml: Elem) = fedora.getRelsExt _ expects * anyNumberOfTimes() returning(xml.toString)

  private def getSolrDocFieldValues(docRoot: Elem, field: String): Seq[String] = {
    (docRoot \\ "doc" \ "field").filter(f => (f \ "@name").text == field).map(_.text)
  }

  /*
   * Tests
   */
  "minimal DC" should "result in only sid field" in {
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml

    docRoot.label should be("add")
    (docRoot \ "doc" \ "field") should have length(2)
    (docRoot \ "doc" \ "field" \\ "@name")(1).text should be ("sid")
    (docRoot \ "doc" \ "field")(1).text should be ("test-pid:123")
  }

  "one DC element" should "result in normal and sortable fields" in {
    expectDc(
      <dc>
        <title>Some title</title>
      </dc>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val fields = (docRoot \ "doc" \ "field").map(f => ((f \ "@name").text -> f.text))

    fields should have length(4)
    fields should contain("sid" -> "test-pid:123")
    fields should contain("dc_title" -> "Some title")
    fields should contain("dc_title_s" -> "Some title")
  }

  "Sort field with multiple values" should "result in concatenated values" in {
    expectDc(
      <dc>
        <dc:title>Title 1</dc:title>
        <dc:title>Title 2</dc:title>
        <dc:description>My description</dc:description>
      </dc>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val sortTitle = getSolrDocFieldValues(docRoot, "dc_title_s")

    sortTitle should have length(1)
    sortTitle should contain("Title 1 Title 2")
  }

  "archaeology_dc_subject" should "only get values from subject fields with corresponding schemeId attribute" in {
    expectEmd(
      <easymetadata
         xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
         <emd:subject>
           <dc:subject eas:scheme="ABR" eas:schemeId="archaeology.dc.subject">ELCF</dc:subject>
           <dc:subject>some other subject</dc:subject>
         </emd:subject>
      </easymetadata>)
      expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val archSubjects= (docRoot \ "doc" \\ "field").filter(f => (f \ "@name").text == "archaeology_dc_subject").map(_.text)

    archSubjects should contain("ELCF")
    archSubjects shouldNot contain("some other subject")
  }

  "archaeology_dcterms_temporal" should "only get values from subject fields with corresponding schemeId attribute" in {
    expectEmd(
      <easymetadata
      xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:coverage>
          <dcterms:temporal>some other temporal</dcterms:temporal>
          <dcterms:temporal eas:scheme="ABR" eas:schemeId="archaeology.dcterms.temporal">MESO</dcterms:temporal>
        </emd:coverage>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val archTemporals = getSolrDocFieldValues(docRoot, "archaeology_dcterms_temporal")

    archTemporals should contain("MESO")
    archTemporals shouldNot contain("some other temporal")
  }

  "dai_creator" should "get value from entityId field with scheme DAI in eas:creator" in {
      val CREATOR_DAI = "123456789"
      expectEmd(
        <easymetadata
        xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
          <emd:creator>
            <eas:creator>
              <eas:entityId scheme="DAI">{CREATOR_DAI}</eas:entityId>
            </eas:creator>
          </emd:creator>
        </easymetadata>)
      expectEmptyXmlByDefault
      val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
      val daiCreators = getSolrDocFieldValues(docRoot, "dai_creator")

      daiCreators should contain(CREATOR_DAI)
  }

  "dai_contributor" should "get value from entityId field with scheme DAI in eas:contributor" in {
    val CONTRIBUTOR_DAI = "123456789"
    expectEmd(
      <easymetadata
      xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:contributor>
          <eas:contributor>
            <eas:entityId scheme="DAI">{CONTRIBUTOR_DAI}</eas:entityId>
          </eas:contributor>
        </emd:contributor>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val daiContributors = getSolrDocFieldValues(docRoot, "dai_contributor")

    daiContributors should contain(CONTRIBUTOR_DAI)
  }

  "mix of dai_creator and dai_contributor" should "not get mixed up" in {
    val CREATOR1_DAI = "123456789"
    val CREATOR2_DAI = "234567891"
    val CONTRIBUTOR1_DAI = "4567890123"
    val CONTRIBUTOR2_DAI = "6789012345"
    expectEmd(
      <easymetadata
      xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:creator>
          <eas:creator>
            <eas:entityId scheme="DAI">{CREATOR1_DAI}</eas:entityId>
          </eas:creator>
        </emd:creator>
        <emd:creator>
          <eas:creator>
            <eas:entityId scheme="DAI">{CREATOR2_DAI}</eas:entityId>
          </eas:creator>
        </emd:creator>
        <emd:contributor>
          <eas:contributor>
            <eas:entityId scheme="DAI">{CONTRIBUTOR1_DAI}</eas:entityId>
          </eas:contributor>
          <eas:contributor>
            <eas:entityId scheme="DAI">{CONTRIBUTOR2_DAI}</eas:entityId>
          </eas:contributor>
        </emd:contributor>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val daiCreators = getSolrDocFieldValues(docRoot, "dai_creator")
    val daiContributors = getSolrDocFieldValues(docRoot, "dai_contributor")

    daiCreators should have length(2)
    daiContributors should have length(2)
    daiCreators should contain(CREATOR1_DAI)
    daiCreators should contain(CREATOR2_DAI)
    daiContributors should contain(CONTRIBUTOR1_DAI)
    daiContributors should contain(CONTRIBUTOR2_DAI)
  }

  "permission request status" should "take values from multiple fields and format it as a string" in {
    val REQUESTER_ID = "R E Quester"
    val STATE = "Granted"
    val LAST_MODIFIED = "2015-05-21T12:02:10.108+02:00"

    expectPrsl(
      <psl:permissionSequenceList xmlns:psl="http://easy.dans.knaw.nl/easy/permission-sequence-list/">
        <sequences>
          <sequence>
            <requesterId>{REQUESTER_ID}</requesterId>
            <state>{STATE}</state>
            <stateLastModified>{LAST_MODIFIED}</stateLastModified>
            <request>
              <lastRequestDate>2015-05-21T11:48:58.699+02:00</lastRequestDate>
              <acceptConditionsOfUse>true</acceptConditionsOfUse>
              <requestTitle>.</requestTitle>
              <requestTheme>.</requestTheme>
            </request>
            <reply>
              <lastReplyDate>2015-05-21T12:02:10.108+02:00</lastReplyDate>
              <replyText>.</replyText>
            </reply>
            <back-log></back-log>
          </sequence>
        </sequences>
      </psl:permissionSequenceList>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val requestStatuses = getSolrDocFieldValues(docRoot, "psl_permission_status")

    requestStatuses should have length(1)
    requestStatuses should contain(s"$REQUESTER_ID $STATE $LAST_MODIFIED")
  }

  "multiple permission request status" should "each be formatted in its own string" in {
    val REQUESTER_ID1 = "R E Quester"
    val STATE1 = "Granted"
    val LAST_MODIFIED1 = "2015-05-21T12:02:10.108+02:00"

    val REQUESTER_ID2 = "N Agger"
    val STATE2 = "Denied"
    val LAST_MODIFIED2 = "2014-05-21T12:02:10.108+02:00"

    val REQUESTER_ID3 = "N Ew"
    val STATE3 = "Submitted"
    val LAST_MODIFIED3 = "2013-05-21T12:02:10.108+02:00"

    expectPrsl(
      <psl:permissionSequenceList xmlns:psl="http://easy.dans.knaw.nl/easy/permission-sequence-list/">
        <sequences>
          <sequence>
            <requesterId>{REQUESTER_ID1}</requesterId>
            <state>{STATE1}</state>
            <stateLastModified>{LAST_MODIFIED1}</stateLastModified>
            <request>
              <lastRequestDate>2015-05-21T11:48:58.699+02:00</lastRequestDate>
              <acceptConditionsOfUse>true</acceptConditionsOfUse>
              <requestTitle>.</requestTitle>
              <requestTheme>.</requestTheme>
            </request>
            <reply>
              <lastReplyDate>2015-05-21T12:02:10.108+02:00</lastReplyDate>
              <replyText>.</replyText>
            </reply>
            <back-log></back-log>
          </sequence>
          <sequence>
            <requesterId>{REQUESTER_ID2}</requesterId>
            <state>{STATE2}</state>
            <stateLastModified>{LAST_MODIFIED2}</stateLastModified>
            <request>
              <lastRequestDate>2015-05-21T11:48:58.699+02:00</lastRequestDate>
              <acceptConditionsOfUse>true</acceptConditionsOfUse>
              <requestTitle>.</requestTitle>
              <requestTheme>.</requestTheme>
            </request>
            <reply>
              <lastReplyDate>2015-05-21T12:02:10.108+02:00</lastReplyDate>
              <replyText>.</replyText>
            </reply>
            <back-log></back-log>
          </sequence>
          <sequence>
            <requesterId>{REQUESTER_ID3}</requesterId>
            <state>{STATE3}</state>
            <stateLastModified>{LAST_MODIFIED3}</stateLastModified>
            <request>
              <lastRequestDate>2015-05-21T11:48:58.699+02:00</lastRequestDate>
              <acceptConditionsOfUse>true</acceptConditionsOfUse>
              <requestTitle>.</requestTitle>
              <requestTheme>.</requestTheme>
            </request>
            <reply>
              <lastReplyDate>2015-05-21T12:02:10.108+02:00</lastReplyDate>
              <replyText>.</replyText>
            </reply>
            <back-log></back-log>
          </sequence>
        </sequences>
      </psl:permissionSequenceList>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val requestStatuses = getSolrDocFieldValues(docRoot, "psl_permission_status")

    requestStatuses should have length(3)
    requestStatuses should contain(s"$REQUESTER_ID3 $STATE3 $LAST_MODIFIED3")
    requestStatuses should contain(s"$REQUESTER_ID2 $STATE2 $LAST_MODIFIED2")
    requestStatuses should contain(s"$REQUESTER_ID1 $STATE1 $LAST_MODIFIED1")
  }

  "easy-collection" should "be read from RELS-EXT" in {
    val COLLECTION = "info:fedora/easy-collection:5"
    val STRIPPED_COLLECTION = "easy-collection:5"

    expectRelsExt(
      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
      <rdf:Description rdf:about="info:fedora/easy-dataset:5058">
        <hasModel xmlns="info:fedora/fedora-system:def/model#" rdf:resource="info:fedora/easy-model:EDM1DATASET"></hasModel>
        <hasModel xmlns="info:fedora/fedora-system:def/model#" rdf:resource="info:fedora/easy-model:oai-item1"></hasModel>
        <hasDoi xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:parseType="Literal">10.5072/dans-zyf-v9sc</hasDoi>
        <isMemberOfOAISet xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:resource="info:fedora/easy-data:oai-driverset1"></isMemberOfOAISet>
        <isCollectionMember xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:resource={COLLECTION}></isCollectionMember>
        <isMemberOfOAISet xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:resource="info:fedora/easy-discipline:14"></isMemberOfOAISet>
        <itemID xmlns="http://www.openarchives.org/OAI/2.0/" rdf:parseType="Literal">oai:easy.dans.knaw.nl:easy-dataset:5058</itemID>
        <hasPid xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:parseType="Literal">urn:nbn:nl:ui:13-o7dy-s5</hasPid>
        <hasModel xmlns="info:fedora/fedora-system:def/model#" rdf:resource="info:fedora/dans-model:recursive-item-v1"></hasModel>
        <isMemberOf xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:resource="info:fedora/easy-discipline:14"></isMemberOf>
      </rdf:Description>
    </rdf:RDF>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val collections = getSolrDocFieldValues(docRoot, "easy_collections")

    collections should have length(1)
    collections should contain(STRIPPED_COLLECTION)
  }


}

