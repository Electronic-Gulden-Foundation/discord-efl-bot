package services.discord.messagehandlers
import services.discord.{TipBotCommand, TipBotMessage}

object HelpMessageHandler {

  def helpMessageText: String = {
    val filterCommands = Seq(TipBotCommand.None, TipBotCommand.Tip)

    val cmdHelp = TipBotCommand.values
      .filterNot(filterCommands.contains)
      .filterNot(_ == null)
      .map(cmd => s"${cmd.command} - ${cmd.helpText}")
      .mkString("- ", "\n- ", "")

    // TODO: Link naar broncode
    s"""
      |Welkom bij de EFL tip bot!
      |
      |${TipBotCommand.Tip.helpText}
      |${TipBotCommand.Tip.sampleText}
      |
      |Commando's: !tip <commando>
      |$cmdHelp
      |
      |""".stripMargin
  }
}

class HelpMessageHandler extends TipBotMessageHandler {

  override def handlesTypes: Seq[TipBotCommand] = Seq(TipBotCommand.Help)

  override def handleMessage(msg: TipBotMessage): Unit = {
    msg.discordMessage.getChannel.sendMessage(HelpMessageHandler.helpMessageText).queue()
  }
}
