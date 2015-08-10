package nl.knaw.dans.easy.solr

import org.scalatest.{Matchers, FlatSpec}

class IsoDateSpec extends FlatSpec
    with Matchers
{

  "Precision YEAR" should "format date leaving off everything but year" in {
    IsoDate.format("2015-02-03T12:34:56.789", "YEAR") should be ("2015")
  }

  "Precision MONTH" should "format date leaving off day and further" in {
    IsoDate.format("2015-02-03T12:34:56.789", "MONTH") should be ("2015-02")
  }

  "Precision DAY" should "format date leaving off hour and further" in {
    IsoDate.format("2015-02-03T12:34:56.789", "DAY") should be ("2015-02-03")
  }

  "Precision HOUR" should "format date leaving off minute and further" in {
    IsoDate.format("2015-02-03T12:34:56.789", "HOUR") should be ("2015-02-03T12")
  }

  "Precision MINUTE" should "format date leaving off second and further" in {
    IsoDate.format("2015-02-03T12:34:56.789", "MINUTE") should be ("2015-02-03T12:34")
  }

  "Precision SECOND" should "format date leaving off millisecond" in {
    IsoDate.format("2015-02-03T12:34:56.789", "SECOND") should be ("2015-02-03T12:34:56")
  }

  "Precision MILLISECOND" should "include everything" in {
    // The time zone depends on where this test is executed, so it is not checked
    IsoDate.format("2015-02-03T12:34:56.789", "MILLISECOND") should startWith ("2015-02-03T12:34:56.789")
  }

  "Precision unspecified" should "default to millisecond" in {
    // The time zone depends on where this test is executed, so it is not checked
    IsoDate.format("2015-02-03T12:34:56.789", "") should startWith ("2015-02-03T12:34:56.789")
  }

}
