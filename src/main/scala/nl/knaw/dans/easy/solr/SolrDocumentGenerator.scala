/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr

import org.joda.time.{DateTimeZone, DateTime}
import org.slf4j.{LoggerFactory, Logger}

import scala.xml._

class SolrDocumentGenerator(fedora: FedoraProvider, pid: String, log: Logger = LoggerFactory.getLogger(getClass)) {
  val DC_NAMESPACE: String = "http://purl.org/dc/elements/1.1/"
  val EAS_NAMESPACE: String = "http://easy.dans.knaw.nl/easy/easymetadata/eas/"
  val RDF_NAMESPACE: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

  //val dc = XML.loadString(fedora.getDc(pid))
  val emd = XML.loadString(fedora.getEmd(pid))
  val amd = XML.loadString(fedora.getAmd(pid))
  val prsl = XML.loadString(fedora.getPrsql(pid))
  val relsExt = XML.loadString(fedora.getRelsExt(pid))

  /** dc */

  /** with sort fields */
  val dcTitleFromEmdMappings = List("title")
    .map(s => s"dc_$s" -> (emd \ s \ "_").map(_.text))
  val dcTitleSort = List("title").map(s => s"dc_${s}_s" -> Seq(dcTitleFromEmdMappings.head._2.mkString(" ")))

  val dcPublisherFromEmdMappings = List("publisher")
    .map(s => s"dc_$s" -> (emd \ s \ "_").map(_.text))
  val dcPublisherSort = List("publisher").map(s => s"dc_${s}_s" -> Seq(dcPublisherFromEmdMappings.head._2.mkString(" ")))

  def extractPersonForDc(p: Node) = {
    // formatting the persons name
    val nameStart = (p \ "surname").text
    val nameEnd = List("title", "initials", "prefix").map(s => (p \ s).text).filter(_.nonEmpty).mkString(" ")
    val name = List(nameStart, nameEnd).filter(_.nonEmpty).mkString(", ")
    val org = (p \ "organization").text

    if (org.isEmpty) name
    else if (name.nonEmpty) s"$name ($org)"
    else org
  }

  val dcCreatorFromEmdMappings = List("creator").map(s => s"dc_$s" -> (emd \ s \ "_")
    .map(n => {
      if ( n.namespace == EAS_NAMESPACE) extractPersonForDc(n)
      else n.text
    })
  )
  val dcCreatorSort = List("creator").map(s => s"dc_${s}_s" -> Seq(dcCreatorFromEmdMappings.head._2.mkString(" ")))

  val dcContributorFromEmdMappings = List("contributor").map(s => s"dc_$s" -> (emd \ s \ "_")
    .map(n => {
      if ( n.namespace == EAS_NAMESPACE) extractPersonForDc(n)
      else n.text
    })
  )
  val dcContributorSort = List("contributor").map(s => s"dc_${s}_s" -> Seq(dcContributorFromEmdMappings.head._2.mkString(" ")))

  /** without sort fields */

  val dcOtherFromEmdMappings = List("description","subject","type","format","identifier","source","language","rights")
    .map(s => s"dc_$s" -> (emd \ s \ "_").map(_.text))

  val dcDateFromEmdMappings = List("date").map(s => s"dc_$s" -> (emd \ s \ "_")
    .map(n => {
      if (isFormattableDate(n)) IsoDate.format(n.text, getPrecision(n))
      else n.text
    }))

  def extractRelationForDc(relation: Node) = {
    (( relation \\ "subject-title").text, (relation \\ "subject-link").text) match {
      case (title, "") => s"title=$title"
      case (title, uri) => s"title=$title URI=$uri"
    }
  }

  val dcRelationFromEmdMappings = List("relation")
    .map(s => s"dc_$s" -> (emd \ s \ "_").map(r => extractRelationForDc(r)))

  def extractPointForDc(point: Node) = {
    s"scheme=${point.attribute(EAS_NAMESPACE, "scheme").get} x=${(point \ "x").text} y=${(point \ "y").text}"
  }

  def extractBoxForDc(box: Node) = {
    val coordinates = List("north", "east", "south", "west").map(cn => s"$cn=${(box \ cn).text}").mkString(" ")
    s"scheme=${box.attribute(EAS_NAMESPACE, "scheme").get} $coordinates"
  }

  def extractSpatialForDc(spatial: Node) = {
    (spatial \ "point", spatial \ "box") match {
      case (Seq(), Seq()) => spatial.text
      case (pointSeq, Seq()) => extractPointForDc(pointSeq.head)
      case (Seq(), boxSeq) => extractBoxForDc(boxSeq.head)
    }
  }

  val dcCoverageFromEmdMappings = List("coverage")
    .map(s => s"dc_$s" -> (emd \ s \ "_").map( n => n.label match {
      case "spatial" => extractSpatialForDc(n)
      case _ => n.text
    }))

  /** combine */

  //  val dcFromEmdMappings = List("title")
  //    .map(s => s"dc_$s" -> (emd \ s).map(_.text))

  //val dcMappings = List("title", "description", "creator", "subject", "publisher",
  //  "contributor", "date", "type", "format", "identifier", "source", "language", "relation",
  //  "coverage", "rights")
  //  .map(s => s"dc_$s" -> (dc \ s).map(_.text))

  val dcMappings = dcTitleFromEmdMappings ++
    dcPublisherFromEmdMappings ++
    dcCreatorFromEmdMappings ++
    dcContributorFromEmdMappings ++
    dcDateFromEmdMappings ++
    dcRelationFromEmdMappings ++
    dcCoverageFromEmdMappings ++
    dcOtherFromEmdMappings

  //val dcMappingsSort = List("title", "creator", "publisher", "contributor")
  //  .map(s => s"dc_${s}_s" -> (dc \\ s).map(_.text))

  val dcMappingsSort = dcCreatorSort ++ dcContributorSort ++ dcTitleSort ++ dcPublisherSort

  /** emd */

  val emdDateMappings = List("created", "available", "submitted", "published", "deleted")
    .map(s => s"emd_date_${s}" -> getEasDateElement(s).map(n => toUtcTimestamp(n.text)))

  def getEasDateElement(typeOfDate: String) = {
    val elements = (emd \ "date" \ typeOfDate).filter(_.namespace == EAS_NAMESPACE)
    if(elements.size > 1) {
      log.warn(s"Found ${elements.size} date $typeOfDate elements but only one should be allowed. Metadata may be wrong! Using the first element found.")
      NodeSeq.fromSeq(elements.head.toSeq)
    } else elements
  }

  def toUtcTimestamp(s: String): String =
    DateTime.parse(s).withZone(DateTimeZone.UTC).toString

  val emdFormattedDateMappings = List("created", "available")
    .map(s => s"emd_date_${s}_formatted" -> getEasDateElement(s).filter(isFormattableDate).map(n => IsoDate.format(n.text, getPrecision(n))))

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
    val required =  n \ "required"
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
    if(values.isEmpty) NodeSeq.Empty
    else <field name={name}>{values.mkString(" ")}</field>

}
