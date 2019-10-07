package nl.egulden.discordbot.services.ticker

import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test.WsTestClient

import scala.concurrent.ExecutionContext.Implicits.global

class EflNlTickerTest extends PlaySpec with WsTestClient {

  "EflNlTickerTest" should {
    "getTickerInfo" in {
      withClient { wsClient =>
        val eflNlTicker = new EflNlTicker(wsClient)

        val result = await(eflNlTicker.getTickerInfo())

        result.eur_market_cap >= 0d mustBe true
      }
    }
  }
}
