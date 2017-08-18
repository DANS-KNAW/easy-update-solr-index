/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr

import org.joda.time.{ DateTime, DateTimeZone }
import org.slf4j.{ Logger, LoggerFactory }

import scala.collection.immutable.Seq
import scala.xml._

class SolrDocumentGenerator(fedora: FedoraProvider, pid: String, log: Logger = LoggerFactory.getLogger(getClass)) {
  val DC_NAMESPACE: String = "http://purl.org/dc/elements/1.1/"
  val EAS_NAMESPACE: String = "http://easy.dans.knaw.nl/easy/easymetadata/eas/"
  val RDF_NAMESPACE: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

  val emd: Elem = XML.loadString(fedora.getEmd(pid))
  val amd: Elem = XML.loadString(fedora.getAmd(pid))
  val prsl: Elem = XML.loadString(fedora.getPrsql(pid))
  val relsExt: Elem = XML.loadString(fedora.getRelsExt(pid))

  /* dc */

  /* with sort fields */

  def extractMappingFromEmd(name: String)(f: Node => String): (String, Seq[String]) = {
    s"dc_$name" -> (emd \ name \ "_").map(f)
  }

  val dcTitleFromEmdMapping @ (dcTitleKey, dcTitleValues) = extractMappingFromEmd("title")(_.text)

  val dcPublisherFromEmdMapping @ (dcPublisherKey, dcPublisherValues) = extractMappingFromEmd("publisher")(_.text)

  def extractPersonOrganizationForDc(p: Node): String = {
    // formatting the person's name
    val nameStart = (p \ "surname").text
    val nameEnd = List("title", "initials", "prefix").map(s => (p \ s).text).filter(_.nonEmpty).mkString(" ")
    val name = List(nameStart, nameEnd).filter(_.nonEmpty).mkString(", ")
    val org = (p \ "organization").text

    if (org.isEmpty) name
    else if (name.nonEmpty) s"$name ($org)"
    else org
  }

  val dcCreatorFromEmdMapping @ (dcCreatorKey, dcCreatorValues) = extractMappingFromEmd("creator")(n => {
    if (n.namespace == EAS_NAMESPACE) extractPersonOrganizationForDc(n)
    else n.text
  })

  val dcContributorFromEmdMapping @ (dcContributorKey, dcContributorValues) = extractMappingFromEmd("contributor")(n => {
    if (n.namespace == EAS_NAMESPACE) extractPersonOrganizationForDc(n)
    else n.text
  })

  /* without sort fields */

  val dcOtherFromEmdMappings: List[(String, Seq[String])] = List("description", "subject", "type", "format", "identifier", "source", "language", "rights")
    .map(s => extractMappingFromEmd(s)(_.text))

  val dcDateFromEmdMapping: (String, Seq[String]) = extractMappingFromEmd("date")(n => {
    if (isFormattableDate(n)) IsoDate.format(n.text, getPrecision(n))
    else n.text
  })

  def extractRelationForDc(relation: Node): String = {
    ((relation \\ "subject-title").text, (relation \\ "subject-link").text) match {
      case (title, "") => s"title=$title"
      case ("", uri) => s"URI=$uri"
      case (title, uri) => s"title=$title URI=$uri"
    }
  }

  val dcRelationFromEmdMapping: (String, Seq[String]) = extractMappingFromEmd("relation")(n => {
    if (n.namespace == EAS_NAMESPACE) extractRelationForDc(n)
    else n.text
  })

  def extractPointForDc(point: Node): String = {
    s"scheme=${ point.attribute(EAS_NAMESPACE, "scheme").get } x=${ (point \ "x").text } y=${ (point \ "y").text }"
  }

  def extractBoxForDc(box: Node): String = {
    val coordinates = List("north", "east", "south", "west").map(cn => s"$cn=${ (box \ cn).text }").mkString(" ")
    s"scheme=${ box.attribute(EAS_NAMESPACE, "scheme").get } $coordinates"
  }

  def extractSpatialForDc(spatial: Node): String = {
    (spatial \ "point", spatial \ "box") match {
      case (Seq(), Seq()) => spatial.text
      case (pointSeq, Seq()) => extractPointForDc(pointSeq.head)
      case (Seq(), boxSeq) => extractBoxForDc(boxSeq.head)
    }
  }

  val dcCoverageFromEmdMapping: (String, Seq[String]) = extractMappingFromEmd("coverage")(n => {
    if (n.label == "spatial") extractSpatialForDc(n)
    else n.text
  })

  /* combine */

  val dcMappings: List[(String, Seq[String])] = dcTitleFromEmdMapping ::
    dcPublisherFromEmdMapping ::
    dcCreatorFromEmdMapping ::
    dcContributorFromEmdMapping ::
    dcDateFromEmdMapping ::
    dcRelationFromEmdMapping ::
    dcCoverageFromEmdMapping ::
    dcOtherFromEmdMappings

  val dcMappingsSort: List[(String, Seq[String])] = (s"${ dcCreatorKey }_s" -> dcCreatorValues) ::
    (s"${ dcContributorKey }_s" -> dcContributorValues) ::
    (s"${ dcTitleKey }_s" -> dcTitleValues) ::
    (s"${ dcPublisherKey }_s" -> dcPublisherValues) ::
    Nil

  /* emd */

  val emdDateMappings: List[(String, Seq[String])] = List("created", "available", "submitted", "published", "deleted")
    .map(s => s"emd_date_$s" -> getEasDateElement(s).map(n => toUtcTimestamp(n.text)))

  def getEasDateElement(typeOfDate: String): NodeSeq = {
    val elements = (emd \ "date" \ typeOfDate).filter(_.namespace == EAS_NAMESPACE)
    if (elements.size > 1) {
      log.warn(s"Found ${ elements.size } date $typeOfDate elements but only one should be allowed. Metadata may be wrong! Using the first element found.")
      NodeSeq.fromSeq(elements.head)
    }
    else elements
  }

  def toUtcTimestamp(s: String): String =
    DateTime.parse(s).withZone(DateTimeZone.UTC).toString

  val emdFormattedDateMappings: List[(String, Seq[String])] = List("created", "available")
    .map(s => s"emd_date_${ s }_formatted" -> getEasDateElement(s).filter(isFormattableDate).map(n => IsoDate.format(n.text, getPrecision(n))))

  def isFormattableDate(n: Node): Boolean =
    (n.attribute(EAS_NAMESPACE, "format"), n.attribute(EAS_NAMESPACE, "scheme")) match {
      case (Some(Seq(_)), Some(Seq(_))) => true
      case _ => false
    }

  def getPrecision(n: Node): String =
    n.attribute(EAS_NAMESPACE, "format") match {
      case Some(Seq(p)) => p.text
      case _ => ""
    }

  val otherMappings =
    List("amd_assignee_id" -> (amd \ "workflowData" \ "assigneeId").map(_.text),
      "amd_depositor_id" -> (amd \ "depositorId").map(_.text),
      "amd_workflow_progress" -> List((amd \ "workflowData" \\ "workflow").count(isRequiredAndCompletedStep).toString),
      "ds_state" -> (amd \ "datasetState").map(_.text),
      "ds_accesscategory" -> (emd \ "rights" \ "accessRights").map(_.text),
      "emd_audience" -> (emd \ "audience" \ "audience").map(_.text),
      "psl_permission_status" -> (prsl \ "sequences" \\ "sequence").map(formatPrslString),
      "archaeology_dc_subject" -> (emd \ "subject" \ "subject").filter(isArchaeologySubject).map(_.text),
      "archaeology_dcterms_temporal" -> (emd \ "coverage" \ "temporal").filter(isArchaeologyTemporal).map(_.text),
      "dai_creator" -> ((emd \\ "creator" \ "creator").filter(_.namespace == EAS_NAMESPACE) \ "entityId").filter(hasDaiScheme).map(_.text),
      "dai_contributor" -> ((emd \\ "contributor" \ "contributor").filter(_.namespace == EAS_NAMESPACE) \ "entityId").filter(hasDaiScheme).map(_.text),
      "easy_collections" -> (relsExt \\ "Description" \ "isCollectionMember")
        .map(_.attribute(RDF_NAMESPACE, "resource") match {
          case Some(attr) => attr.text.replace("info:fedora/", "")
          case _ => ""
        }))

  def isRequiredAndCompletedStep(n: Node): Boolean = {
    val required = n \ "required"
    val completed = n \ "completed"
    List(required, completed).forall(p => p.nonEmpty && p.text == "true")
  }

  def hasDaiScheme(n: Node): Boolean =
    n.attribute("scheme") match {
      case Some(Seq(s)) => s.text == "DAI"
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
    <doc>
      <!-- Some standard fields that need to be here, in this order. Don't ask ... -->
      <field name="type">easy-dataset</field>
      <field name="type">dataset</field>
      <field name="repository_id">easy</field>

      <!-- Fields based on metadata -->
      <field name="sid">{pid}</field>
      {dcMappings.flatMap(f => createField(f._1, f._2))}
      {dcMappingsSort.flatMap(f => createSortField(f._1, f._2))}
      {emdDateMappings.flatMap(f => createField(f._1, f._2))}
      {emdFormattedDateMappings.flatMap(f => createField(f._1, f._2))}
      {otherMappings.flatMap(f => createField(f._1, f._2))}
    </doc>

  def createField(name: String, values: Seq[String]): NodeSeq =
    values.map(value => <field name={name}>{value}</field>)

  def createSortField(name: String, values: Seq[String]): NodeSeq =
    if (values.isEmpty) NodeSeq.Empty
    else <field name={name}>{values.mkString(" ")}</field>

}