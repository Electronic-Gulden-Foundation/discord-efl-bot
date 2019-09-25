package nl.egulden.discordbot.services.discord.messagehandlers

import java.text.DecimalFormat

import javax.inject.Inject
import nl.egulden.discordbot.services.discord.Command.Command
import nl.egulden.discordbot.services.discord.{BotMessage, Command}
import nl.egulden.discordbot.services.ticker.EflNlTicker

import scala.concurrent.ExecutionContext

class TickerMessageHandler @Inject()(eflNlTicker: EflNlTicker)
                                    (implicit ec: ExecutionContext)
  extends TipBotMessageHandler {

  val max5DecimalFormat = new DecimalFormat("#.#####")
  val max8DecimalFormat = new DecimalFormat("#.########")

  override def handlesTypes: Seq[Command] = Seq(Command.Ticker)

  override def handleMessage(botMessage: BotMessage): Unit = {
    eflNlTicker.getTickerInfo().map { tickerInfo =>
      val msg = String.format(
        s"""
           |EFL (€): %s
           |EFL (Ƀ): %s
           |BTC (€): %.2f
           |24u Volume (€): %.2f
           |Marktkapitalisatie (€): %.2f
           |""".stripMargin,
        max5DecimalFormat.format(tickerInfo.elfprijs_eu),
        max8DecimalFormat.format(tickerInfo.elfprijs_btc),
        tickerInfo.bitcoinprijs_euro,
        tickerInfo.eur_24h_vol,
        tickerInfo.eur_market_cap
      )

      botMessage.message.getChannel.sendMessage(msg).queue()
    }
  }
}
