package nl.egulden.discordbot.services.ticker

import javax.inject.Inject
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}


case class EflNlTickerResult(ltc: Double,
                             btcusd: Double,
                             eur_market_cap: Double,
                             eur_24h_vol: Double,
                             laatste_blok: Long,
                             difficulty: Double,
                             bitcoinprijs_euro: Double,
                             hoogste_biedprijs_marktnaam: String,
                             hoogste_biedprijs_prijs: Double,
                             laagste_vraagprijs_marktnaam: String,
                             laagste_vraagprijs_prijs: Double,
                             elfprijs_btc: Double,
                             elfprijs_eu: Double,
                             euprijs_efl: Double)

object EflNlTicker {
  val URL = "https://a1.efl.nl/efl/s_poll2.php"

  implicit val eflNlTickerResultReads: Reads[EflNlTickerResult] = (
    (JsPath \ "ltc").read[String].map(_.toDouble) and
      (JsPath \ "btcusd").read[String].map(_.toDouble) and
      (JsPath \ "eur_market_cap").read[String].map(_.toDouble) and
      (JsPath \ "eur_24h_vol").read[String].map(_.toDouble) and
      (JsPath \ "laatste_blok").read[String].map(_.toLong) and
      (JsPath \ "difficulty").read[String].map(_.replaceAll(",", "")).map(_.toDouble) and
      (JsPath \ "bitcoinprijs_euro").read[String].map(_.toDouble) and
      (JsPath \ "hoogste_biedprijs_marktnaam").read[String] and
      (JsPath \ "hoogste_biedprijs_prijs").read[String].map(_.toDouble) and
      (JsPath \ "laagste_vraagprijs_marktnaam").read[String] and
      (JsPath \ "laagste_vraagprijs_prijs").read[String].map(_.toDouble) and
      (JsPath \ "elfprijs_btc").read[String].map(_.toDouble) and
      (JsPath \ "elfprijs_eu").read[String].map(_.toDouble) and
      (JsPath \ "euprijs_efl").read[String].map(_.toDouble)
    )(EflNlTickerResult.apply _)
}

class EflNlTicker @Inject()(ws: WSClient) {
  import EflNlTicker._

  def getTickerInfo()(implicit ec: ExecutionContext): Future[EflNlTickerResult] =
    ws.url(URL).get()
      .map { response =>
        response.json.as[EflNlTickerResult]
      }
}
