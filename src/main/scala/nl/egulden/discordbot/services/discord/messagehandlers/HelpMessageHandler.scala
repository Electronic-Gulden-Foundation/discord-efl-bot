package nl.egulden.discordbot.services.discord.messagehandlers

import javax.inject.Inject
import nl.egulden.discordbot.services.discord.Command.Command
import nl.egulden.discordbot.services.discord.{BotMessage, Command, CommandParser}

import scala.concurrent.ExecutionContext

class HelpMessageHandler @Inject()()
  extends TipBotMessageHandler {

  override def handlesTypes: Seq[Command] = Seq(Command.Help)

  override def handleMessage(msg: BotMessage): Unit = {
    msg.message.getChannel.sendMessage(CommandParser.usage()).queue()
  }
}
