package services.discord.messagehandlers
import services.discord.{TipBotCommand, TipBotMessage}

class NoneMessageHandler extends TipBotMessageHandler {
  override def handlesTypes: Seq[TipBotCommand] = Seq(TipBotCommand.None)

  override def handleMessage(msg: TipBotMessage): Unit = {
    if (msg.discordMessage.getContentDisplay.startsWith("!tip")) {
      msg.discordMessage.getChannel.sendMessage(HelpMessageHandler.helpMessageText).queue()
    }
  }
}
