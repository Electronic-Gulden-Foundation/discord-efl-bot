package nl.egulden.discordbot.services.discord.messagehandlers

import nl.egulden.discordbot.services.discord.BotMessage
import nl.egulden.discordbot.services.discord.Command.Command
import play.api.Logger

trait TipBotMessageHandler {
  val logger = Logger(getClass)

  def handlesTypes: Seq[Command]
  def handleMessage(msg: BotMessage): Unit

  final def handles(msg: BotMessage): Boolean =
    handlesTypes.contains(msg.config.command)
}
