/*******************************************************************************
  * Copyright 2015 DANS - Data Archiving and Networked Services
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  ******************************************************************************/

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
