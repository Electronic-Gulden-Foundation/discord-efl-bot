package services.discord

import net.dv8tion.jda.api.entities.Message

class InvalidMessageException(message: TipBotMessage) extends Exception

sealed abstract class TipBotCommand(val command: String,
                                    val minArguments: Int = 0,
                                    val helpText: String = "",
                                    val aliases: Seq[String] = Seq.empty,
                                    val sampleText: String = "") {
  override def equals(obj: Any): Boolean =
    obj != null &&
      obj.isInstanceOf[TipBotCommand] &&
      obj.asInstanceOf[TipBotCommand].command == command
}

object TipBotCommand {
  case object None extends TipBotCommand(command = "")

  case object Address extends TipBotCommand(
    command = "adres",
    helpText = "Vraag je eigen adres op",
    aliases = Seq("adress", "address"),
    sampleText = "!tip adres"
  )
  case object Help extends TipBotCommand(
    command = "help",
    helpText = "Schrijf hulp tekst",
    sampleText = "!tip help",
    aliases = Seq("hulp")
  )
  case object Tip extends TipBotCommand(
    command = "tip",
    minArguments = 2,
    helpText = "Verstuur een tip naar een gebruiker. Het bericht is optioneel",
    sampleText = "!tip @<gebruiker> <aantal> (<bericht>)"
  )

  final val values: Seq[TipBotCommand] = Seq(
    TipBotCommand.None,
    TipBotCommand.Tip,
    TipBotCommand.Help,
    TipBotCommand.Address
  )

  def withName(name: String): Option[TipBotCommand] =
    values.find(cmd => (Seq(cmd.command) ++ cmd.aliases).contains(name))
}

case class TipBotMessage(discordMessage: Message) {

  def cmd: TipBotCommand = {
    if (firstWordIsMention) {
      TipBotCommand.Tip
    } else {
      parsedMessageContents.headOption
        .flatMap(head => TipBotCommand.withName(head.toLowerCase))
        .getOrElse(TipBotCommand.None)
    }
  }

  def args: Seq[String] = {
    if (cmd == TipBotCommand.Tip) {
      parsedMessageContents
    } else if (parsedMessageContents.nonEmpty) {
      parsedMessageContents.tail
    } else {
      Seq.empty
    }
  }

  def isValid: Boolean = {
    args.length >= cmd.minArguments
  }

  def firstWordIsMention: Boolean = {
    val firstWord = parsedMessageContents.headOption

    firstWord.exists(_.startsWith("@"))
  }

  def parsedMessageContents: Seq[String] = {
    discordMessage.getContentDisplay
      .replaceAll("!tip", "")
      .split("\n")
      .head
      .trim
      .split(" ")
      .map(_.trim)
      .filterNot(_.isEmpty)
  }

  override def toString: String = {
    s"TipBotMessage($cmd, $isValid, $args)"
  }
}
