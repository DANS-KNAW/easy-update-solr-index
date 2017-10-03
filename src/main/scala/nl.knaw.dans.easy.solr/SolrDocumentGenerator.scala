/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.{ DateTime, DateTimeZone }

import scala.collection.immutable.Seq
import scala.util.Try
import scala.xml._

object SolrDocumentGenerator {
  def apply(fedora: FedoraProvider, pid: String): Try[SolrDocumentGenerator] = {
    for {
      emdXml <- fedora.getEmd(pid).map(XML.loadString)
      amdXml <- fedora.getAmd(pid).map(XML.loadString)
      prslXml <- fedora.getPrsql(pid).map(XML.loadString)
      relsExtXml <- fedora.getRelsExt(pid).map(XML.loadString)
    } yield new SolrDocumentGenerator(pid) {
      override val emd: Elem = emdXml
      override val amd: Elem = amdXml
      override val relsExt: Elem = relsExtXml
      override val prsl: Elem = prslXml
    }
  }
}
abstract class SolrDocumentGenerator(pid: String) extends DebugEnhancedLogging {
  val DC_NAMESPACE: String = "http://purl.org/dc/elements/1.1/"
  val EAS_NAMESPACE: String = "http://easy.dans.knaw.nl/easy/easymetadata/eas/"
  val RDF_NAMESPACE: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

  val emd: Elem
  val amd: Elem
  val prsl: Elem
  val relsExt: Elem

  /* dc */

  /* with sort fields */

  def extractMappingFromEmd(name: String)(f: Node => String): (String, Seq[String]) = {
    s"dc_$name" -> (emd \ name \ "_").map(f).filter(_.nonEmpty)
  }

  lazy val dcTitleFromEmdMapping @ (dcTitleKey, dcTitleValues) = {
    extractMappingFromEmd("title")(_.text)
  }

  lazy val dcPublisherFromEmdMapping @ (dcPublisherKey, dcPublisherValues) = {
    extractMappingFromEmd("publisher")(_.text)
  }

  def extractPersonOrganizationForDc(p: Node): String = {
    // formatting the person's name
    val nameStart = (p \ "surname").text
    val nameEnd = List("title", "initials", "prefix")
      .map(s => (p \ s).text)
      .filter(_.nonEmpty)
      .mkString(" ")
    val name = List(nameStart, nameEnd).filter(_.nonEmpty).mkString(", ")
    val role = (p \ "role").text
    val org = (p \ "organization").text

    (name, role, org) match {
      case ("", "", "") => ""
      case ("", "", o) => o
      case ("", r, "") => r
      case ("", r, o) => s"$o, $r"
      case (n, "", "") => n
      case (n, "", o) => s"$n ($o)"
      case (n, r, "") => s"$n, $r"
      case (n, r, o) => s"$n, $r ($o)"
    }
  }

  lazy val dcCreatorFromEmdMapping @ (dcCreatorKey, dcCreatorValues) = {
    extractMappingFromEmd("creator") {
      case n if n.namespace == EAS_NAMESPACE => extractPersonOrganizationForDc(n)
      case n => n.text
    }
  }

  lazy val dcContributorFromEmdMapping @ (dcContributorKey, dcContributorValues) = {
    extractMappingFromEmd("contributor") {
      case n if n.namespace == EAS_NAMESPACE => extractPersonOrganizationForDc(n)
      case n => n.text
    }
  }

  /* without sort fields */

  lazy val dcOtherFromEmdMappings: List[(String, Seq[String])] = {
    List("description", "subject", "type", "format", "identifier", "source", "language", "rights")
      .map(extractMappingFromEmd(_)(_.text))
  }

  lazy val dcDateFromEmdMapping: (String, Seq[String]) = {
    extractMappingFromEmd("date") {
      case n if isFormattableDate(n) => IsoDate.format(n.text, getPrecision(n))
      case n => n.text
    }
  }

  def extractRelationForDc(relation: Node): String = {
    ((relation \\ "subject-title").text, (relation \\ "subject-link").text) match {
      case (title, "") => s"title=$title"
      case ("", uri) => s"URI=$uri"
      case (title, uri) => s"title=$title URI=$uri"
    }
  }

  lazy val dcRelationFromEmdMapping: (String, Seq[String]) = {
    extractMappingFromEmd("relation") {
      case n if n.namespace == EAS_NAMESPACE => extractRelationForDc(n)
      case n => n.text
    }
  }

  def extractPlaceForDC(spatial: Node): String = {
    (spatial \ "place").map(_.text).withFilter(_.nonEmpty).map(place => s"place=$place").mkString(", ")
  }

  def extractPointForDc(point: Node): String = {
    s"scheme=${ point.attribute(EAS_NAMESPACE, "scheme").get } x=${ (point \ "x").text } y=${ (point \ "y").text }"
  }

  def extractBoxForDc(box: Node): String = {
    val coordinates = List("north", "east", "south", "west")
      .map(cn => s"$cn=${ (box \ cn).text }")
      .mkString(" ")
    s"scheme=${ box.attribute(EAS_NAMESPACE, "scheme").get } $coordinates"
  }

  def extractPolygonForDc(polygon: Node): String = {
    (polygon \\ "place").map(_.text).withFilter(_.nonEmpty).map(place => s"place=$place").mkString(", ")
  }

  def extractSpatialForDc(spatial: Node): String = {
    ((spatial \ "point", spatial \ "box", spatial \ "polygon") match {
      case (Seq(), Seq(), Seq()) => spatial.text :: Nil
      case (Seq(point, _ @ _*), Seq(), Seq()) => extractPlaceForDC(spatial) :: extractPointForDc(point) :: Nil
      case (Seq(), Seq(box, _ @ _*), Seq()) => extractPlaceForDC(spatial) :: extractBoxForDc(box) :: Nil
      case (Seq(), Seq(), Seq(polygon, _ @ _*)) => extractPlaceForDC(spatial) :: extractPolygonForDc(polygon) :: Nil
      case (Seq(), Seq(), _) => List.empty
      /*
       To future developers: we do currently not index a polygon, even though this kind of 'Spatial'
       was added to DDM, EMD, etc. for the PAN use case. If we want to index polygons in the future,
       that's fine, as long as you keep the following thing in mind:
       - PAN sends us a polygon of the town (gemeente) in which an object is found, and we are NOT!!!
         allowed to convert this to a point in order to show this on our map. If you want to index
         this polygon, make sure it is never/nowhere used as a specific point. This currently also
         includes boxes, as they get converted to a center coordinate in our current map
         implementation.
       */
    }).filter(_.nonEmpty).mkString(", ")
  }

  lazy val dcCoverageFromEmdMapping: (String, Seq[String]) = {
    extractMappingFromEmd("coverage") {
      case n if n.label == "spatial" => extractSpatialForDc(n)
      case n => n.text
    }
  }

  /* combine */

  lazy val dcMappings: List[(String, Seq[String])] = {
    dcTitleFromEmdMapping ::
      dcPublisherFromEmdMapping ::
      dcCreatorFromEmdMapping ::
      dcContributorFromEmdMapping ::
      dcDateFromEmdMapping ::
      dcRelationFromEmdMapping ::
      dcCoverageFromEmdMapping ::
      dcOtherFromEmdMappings
  }

  lazy val dcMappingsSort: List[(String, Seq[String])] = {
    List(
      s"${ dcCreatorKey }_s" -> dcCreatorValues,
      s"${ dcContributorKey }_s" -> dcContributorValues,
      s"${ dcTitleKey }_s" -> dcTitleValues,
      s"${ dcPublisherKey }_s" -> dcPublisherValues
    )
  }

  /* emd */

  lazy val emdDateMappings: List[(String, Seq[String])] = {
    List("created", "available", "submitted", "published", "deleted")
      .map(s => s"emd_date_$s" -> getEasDateElement(s).map(n => toUtcTimestamp(n.text)))
  }

  def getEasDateElement(typeOfDate: String): NodeSeq = {
    (emd \ "date" \ typeOfDate).filter(_.namespace == EAS_NAMESPACE) match {
      case es @ Seq(element, tail @ _*) =>
        val size = es.size
        if (size > 1)
          logger.warn(s"Found $size date $typeOfDate elements but only one should be allowed. Metadata may be wrong! Using the first element found.")

        NodeSeq.fromSeq(element)
      case e => e
    }
  }

  def toUtcTimestamp(s: String): String = DateTime.parse(s).withZone(DateTimeZone.UTC).toString

  lazy val emdFormattedDateMappings: List[(String, Seq[String])] = {
    List("created", "available")
      .map(s => s"emd_date_${ s }_formatted" ->
        getEasDateElement(s)
          .withFilter(isFormattableDate)
          .map(n => IsoDate.format(n.text, getPrecision(n))))
  }

  def isFormattableDate(n: Node): Boolean = {
    (n.attribute(EAS_NAMESPACE, "format"), n.attribute(EAS_NAMESPACE, "scheme")) match {
      case (Some(Seq(_)), Some(Seq(_))) => true
      case _ => false
    }
  }

  def getPrecision(n: Node): String = {
    n.attribute(EAS_NAMESPACE, "format") match {
      case Some(Seq(p)) => p.text
      case _ => ""
    }
  }

  lazy val otherMappings: List[(String, Seq[String])] = {
    List(
      "amd_assignee_id" -> (amd \ "workflowData" \ "assigneeId").map(_.text),
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
        .map(_.attribute(RDF_NAMESPACE, "resource").fold("")(_.text.replace("info:fedora/", "")))
    )
  }

  def isRequiredAndCompletedStep(n: Node): Boolean = {
    val required = n \ "required"
    val completed = n \ "completed"
    List(required, completed).forall(p => p.nonEmpty && p.text == "true")
  }

  def hasDaiScheme(n: Node): Boolean = {
    n.attribute(EAS_NAMESPACE, "scheme") match {
      case Some(Seq(s)) => s.text == "DAI"
      case _ => false
    }
  }

  def isArchaeologySubject(n: Node): Boolean = {
    (n.attribute(EAS_NAMESPACE, "scheme"), n.attribute(EAS_NAMESPACE, "schemeId")) match {
      case (Some(Seq(scheme)), Some(Seq(schemeId))) => scheme.text == "ABR" && schemeId.text == "archaeology.dc.subject"
      case _ => false
    }
  }

  def isArchaeologyTemporal(n: Node): Boolean = {
    (n.attribute(EAS_NAMESPACE, "scheme"), n.attribute(EAS_NAMESPACE, "schemeId")) match {
      case (Some(Seq(scheme)), Some(Seq(schemeId))) => scheme.text == "ABR" && schemeId.text == "archaeology.dcterms.temporal"
      case _ => false
    }
  }

  def formatPrslString(n: Node): String = {
    List(
      (n \ "requesterId").text,
      (n \ "state").text,
      (n \ "stateLastModified").text
    ).mkString(" ")
  }

  def toXml: Elem = {
    <doc>
      <!-- Some standard fields that need to be here, in this order. Don't ask ... -->
      <field name="type">easy-dataset</field>
      <field name="type">dataset</field>
      <field name="repository_id">easy</field>

      <!-- Fields based on metadata -->
      <field name="sid">{pid}</field>
      {dcMappings.flatMap((createField _).tupled)}
      {dcMappingsSort.flatMap((createSortField _).tupled)}
      {emdDateMappings.flatMap((createField _).tupled)}
      {emdFormattedDateMappings.flatMap((createField _).tupled)}
      {otherMappings.flatMap((createField _).tupled)}
    </doc>
  }

  def createField(name: String, values: Seq[String]): NodeSeq = {
    values.map(value => <field name={name}>{value}</field>)
  }

  def createSortField(name: String, values: Seq[String]): NodeSeq = {
    values match {
      case Seq() => NodeSeq.Empty
      case vs => <field name={name}>{vs.mkString(" ")}</field>
    }
  }
}
