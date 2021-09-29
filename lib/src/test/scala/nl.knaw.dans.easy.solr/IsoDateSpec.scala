/*
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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IsoDateSpec extends AnyFlatSpec with Matchers {

  "Precision YEAR" should "format date leaving off everything but year" in {
    IsoDate.format("2015-02-03T12:34:56.789", "YEAR") shouldBe "2015"
  }

  "Precision MONTH" should "format date leaving off day and further" in {
    IsoDate.format("2015-02-03T12:34:56.789", "MONTH") shouldBe "2015-02"
  }

  "Precision DAY" should "format date leaving off hour and further" in {
    IsoDate.format("2015-02-03T12:34:56.789", "DAY") shouldBe "2015-02-03"
  }

  "Precision HOUR" should "format date leaving off minute and further" in {
    IsoDate.format("2015-02-03T12:34:56.789", "HOUR") shouldBe "2015-02-03T12"
  }

  "Precision MINUTE" should "format date leaving off second and further" in {
    IsoDate.format("2015-02-03T12:34:56.789", "MINUTE") shouldBe "2015-02-03T12:34"
  }

  "Precision SECOND" should "format date leaving off millisecond" in {
    IsoDate.format("2015-02-03T12:34:56.789", "SECOND") shouldBe "2015-02-03T12:34:56"
  }

  "Precision MILLISECOND" should "include everything" in {
    // The time zone depends on where this test is executed, so it is not checked
    IsoDate.format("2015-02-03T12:34:56.789", "MILLISECOND") should startWith("2015-02-03T12:34:56.789")
  }

  "Precision unspecified" should "default to millisecond" in {
    // The time zone depends on where this test is executed, so it is not checked
    IsoDate.format("2015-02-03T12:34:56.789", "") should startWith("2015-02-03T12:34:56.789")
  }
}
