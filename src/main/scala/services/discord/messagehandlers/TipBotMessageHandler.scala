package services.discord.messagehandlers

import play.api.Logger
import services.discord.{TipBotCommand, TipBotMessage}

trait TipBotMessageHandler {
  val logger = Logger(getClass)

  def handlesTypes: Seq[TipBotCommand]
  def handleMessage(msg: TipBotMessage): Unit

  final def handles(msg: TipBotMessage): Boolean =
    msg.isValid && handlesTypes.contains(msg.cmd)
}
