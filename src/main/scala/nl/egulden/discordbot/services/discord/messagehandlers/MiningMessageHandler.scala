package nl.egulden.discordbot.services.discord.messagehandlers

import javax.inject.{Inject, Named}
import nl.egulden.discordbot.services.bitcoinrpc.BitcoinRpcClient
import nl.egulden.discordbot.services.discord.Command.Command
import nl.egulden.discordbot.services.discord.{BotMessage, Command}

import scala.concurrent.ExecutionContext

class MiningMessageHandler @Inject()(@Named("egulden") eguldenRpcClient: BitcoinRpcClient)
                                    (implicit ec: ExecutionContext)
  extends TipBotMessageHandler {

  override def handlesTypes: Seq[Command] = Seq(Command.Mining)

  override def handleMessage(botMessage: BotMessage): Unit = {
    eguldenRpcClient.getMiningInfo().map { miningInfo =>
      val msg =
        s"""
           |Hashrate: ${getNiceHashRate(miningInfo.networkHashps())}
           |Difficulty: ${miningInfo.difficulty()}
           |Blokhoogte: ${miningInfo.blocks()}
           |""".stripMargin

      botMessage.message.getChannel.sendMessage(msg).queue()
    }
  }

  def getNiceHashRate(hashRate: BigDecimal): String = {
    val format = "%.4f %s"

    if (hashRate / 1e9 > 1) {
      String.format(format, (hashRate / 1e9).toDouble, "GH/s")
    } else if (hashRate / 1e6 > 1) {
      String.format(format, (hashRate / 1e6).toDouble, "MH/s")
    } else {
      String.format(format, (hashRate / 1e3).toDouble, "kH/s")
    }
  }
}
