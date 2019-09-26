package nl.egulden.discordbot.utils.bitcoin

import org.scalatestplus.play.PlaySpec
import nl.egulden.discordbot.utils.bitcoin.SatoshiBigDecimal._

class SatoshiBigDecimalTest extends PlaySpec {

  "SatoshiBigDecimal" should {
    "convert doubles" in {
      1d.satoshis mustBe 1e-8
      1234d.satoshis mustBe 0.00001234
      12345678d.satoshis mustBe 0.12345678
      100000000d.satoshis mustBe 1.00000000
      800000000.satoshis mustBe 8.00000000
    }

    "convert ints" in {
      1.satoshis mustBe 1e-8
      1234.satoshis mustBe 0.00001234
      12345678.satoshis mustBe 0.12345678
      100000000.satoshis mustBe 1.00000000
      800000000.satoshis mustBe 8.00000000
    }

    "convert BigDecimals" in {
      BigDecimal(1).satoshis mustBe 1e-8
      BigDecimal(1234).satoshis mustBe 0.00001234
      BigDecimal(12345678).satoshis mustBe 0.12345678
      BigDecimal(100000000).satoshis mustBe 1.00000000
      BigDecimal(800000000).satoshis mustBe 8.00000000
    }
  }
}
