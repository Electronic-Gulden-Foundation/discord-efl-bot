package nl.egulden.discordbot.services.discord

import java.io.ByteArrayOutputStream

import nl.egulden.discordbot.services.discord.Command.Command
import nl.egulden.discordbot.services.discord.SubCommand.SubCommand
import scopt.{OParser, RenderingMode}

case class CommandConfig(command: Command = Command.Help,
                         subCommand: Option[SubCommand] = None,
                         name: Option[String] = None,
                         amount: Option[Double] = None,
                         isPrivateMessage: Boolean = false)

object Command extends Enumeration {
  type Command = Value

  val Help = Value("!help")
  val Mining = Value("!mining")
  val Ticker = Value("!ticker")
  val Tip = Value("!tip")
}

object SubCommand extends Enumeration {
  type SubCommand = Value

  val Address = Value("adres")
  val Balance = Value("balans")
}

object CommandParser {

  def parser(): OParser[Unit, CommandConfig] = {
    val builder = OParser.builder[CommandConfig]
    import builder._

    OParser.sequence(
      head(
        """
          |Welkom bij de EFL bot! Je kunt deze bot gebruiken om een aantal statistieken te bekijken en om andere gebruikers te 'tippen' als je vind dat ze dat verdienen
          |""".stripMargin
      ),
      cmd(s"${Command.Help}")
        .text("Toon deze hulp tekst")
        .action((_, c) => c.copy(command = Command.Help)),
      cmd(s"${Command.Ticker}")
        .text("Ticker informatie (marktprijzen, etc)")
        .action((_, c) => c.copy(command = Command.Ticker)),
      cmd(s"${Command.Mining}")
        .text("Mining informatie (difficulty, hashrate, etc)")
        .action((_, c) => c.copy(command = Command.Mining)),
      cmd(s"${Command.Tip}")
        .text("Tip een gebruiker")
        .action((_, c) => c.copy(command = Command.Tip))
        .children(
          arg[String]("<@Gebruiker>")
            .text("De gebruiker die je wilt tippen")
            .optional()
            .action((s, c) => c.copy(name = Some(s))),
          arg[Double]("<aantal>")
            .text("Aantal EFL dat je wilt geven")
            .optional()
            .action((a, c) => c.copy(amount = Some(a))),
          cmd(s"${SubCommand.Address}")
            .text("Vraag je adres op")
            .action((_, c) => c.copy(subCommand = Some(SubCommand.Address))),
          cmd(s"${SubCommand.Balance}")
            .text("Vraag je balans op")
            .action((_, c) => c.copy(subCommand = Some(SubCommand.Balance))),
        )
    )
  }

  def usage(): String =
    OParser.usage(parser(), RenderingMode.TwoColumns)
      .replaceAll("Command:", "Bericht:")
      .replaceAll("(?m)^Usage:.*\n", "")
      .replaceAll("(?m)(!tip )\\[[a-zA-Z|]+\\] ", "$1")
      .replaceAll("(?m)^(Bericht:.*)", "\n**$1**")
      .replaceAll("\n\n\n", "\n\n") +
      "\n\nhttps://gitlab.com/electronic-gulden-foundation/discord-efl-bot/"

  def parse(arguments: String): Either[CommandConfig, String] = {
    parse(
      arguments
        .split(" ")
        .map(_.trim)
        .filter(_.nonEmpty)
    )
  }

  def parse(arguments: Array[String]): Either[CommandConfig, String] = {
    val outCapture = new ByteArrayOutputStream
    val errCapture = new ByteArrayOutputStream

    Console.withOut(outCapture) {
      Console.withErr(errCapture) {
        OParser.parse(parser(), arguments, CommandConfig())
          .map(Left(_))
          .getOrElse(Right(errCapture.toString))
      }
    }
  }
}
