package nl.egulden.discordbot.services.ticker

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.ws.WSClient
import play.api.test.Injecting

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class EflNlTickerTest
  extends PlaySpec
    with GuiceOneAppPerSuite
    with Injecting {

  val wsClient = inject[WSClient]
  implicit val ec = inject[ExecutionContext]

  "EflNlTickerTest" should {
    "getTickerInfo" in {
      val eflNlTicker = new EflNlTicker(wsClient)

      val result = Await.result(eflNlTicker.getTickerInfo(), Duration.Inf)

      result.eur_market_cap >= 0d mustBe true
    }
  }
}
