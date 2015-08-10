package nl.knaw.dans.easy.solr

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat


object IsoDate {
  def format(s: String, p: String): String = {
    def formatWithPattern(s: String, pat: String): String =
      DateTime.parse(s).toString(DateTimeFormat.forPattern(pat))
    p match {
      case "YEAR" => formatWithPattern(s, "yyyy")
      case "MONTH" => formatWithPattern(s, "yyyy-MM")
      case "DAY" => formatWithPattern(s, "yyyy-MM-dd")
      case "HOUR" => formatWithPattern(s, "yyyy-MM-dd'T'HH")
      case "MINUTE" => formatWithPattern(s, "yyyy-MM-dd'T'HH:mm")
      case "SECOND" => formatWithPattern(s, "yyyy-MM-dd'T'HH:mm:ss")
      case _ => formatWithPattern(s, "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }
  }
}
