package nl.egulden.discordbot.services.discord.messagehandlers
import javax.inject.Inject
import nl.egulden.discordbot.services.discord.{BotMessage, Command}
import nl.egulden.discordbot.services.discord.Command.Command
import nl.egulden.discordbot.services.ticker.EflNlTicker

import scala.concurrent.ExecutionContext

class MiningMessageHandler @Inject()(eflNlTicker: EflNlTicker)
                                    (implicit ec: ExecutionContext)
  extends TipBotMessageHandler {

  override def handlesTypes: Seq[Command] = Seq(Command.Mining)

  override def handleMessage(botMessage: BotMessage): Unit = {
    eflNlTicker.getTickerInfo().map { tickerInfo =>
      val msg = String.format(
        s"""
           |Hashrate: ${tickerInfo.difficulty}
           |Blokhoogte: ${tickerInfo.laatste_blok}
           |""".stripMargin
      )

      botMessage.message.getChannel.sendMessage(msg).queue()
    }
  }
}
