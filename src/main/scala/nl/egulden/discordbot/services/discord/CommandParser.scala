package nl.egulden.discordbot.services.discord

import java.io.ByteArrayOutputStream
import java.util.NoSuchElementException

import nl.egulden.discordbot.GlobalSettings
import nl.egulden.discordbot.services.discord.Command.Command
import play.api.Logger
import scopt.{OParser, RenderingMode}

case class CommandConfig(command: Command = Command.Help,
                         name: Option[String] = None,
                         amount: Option[Double] = None,
                         address: Option[String] = None,
                         isPrivateMessage: Boolean = false)

object Command extends Enumeration {
  type Command = Value

  val Help = Value("!help")
  val Mining = Value("!mining")
  val Ticker = Value("!ticker")
  val Tip = Value("!tip")
  val TipAddress = Value("!tip adres")
  val TipBalance = Value("!tip balans")
  val TipWithdraw = Value("!tip opnemen")

  val TipCommands = Seq(Tip, TipAddress, TipBalance, TipWithdraw)

  def maybeWithName(name: String): Option[Value] =
    try {
      Some(Command.withName(name))
    } catch  {
      case e: NoSuchElementException => None
      case e: Throwable => throw e
    }
}

object CommandParser {
  private val logger = Logger(getClass)

  def parser(): OParser[Unit, CommandConfig] = {
    val builder = OParser.builder[CommandConfig]
    import builder._

    OParser.sequence(
      head(
        """
          |Welkom bij de EFL bot! Je kunt deze bot gebruiken om een aantal statistieken te bekijken en om andere
          |gebruikers te 'tippen' als je vindt dat ze dat verdienen
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
            .action((s, c) => c.copy(name = Some(s))),
          arg[Double]("<aantal>")
            .text("Aantal EFL dat je wilt geven")
            .action((a, c) => c.copy(amount = Some(a))),
        ),
      cmd(s"${Command.TipAddress}")
        .text("Vraag je adres op")
        .action((_, c) => c.copy(command = Command.TipAddress)),
      cmd(s"${Command.TipBalance}")
        .text("Vraag je balans op")
        .action((_, c) => c.copy(command = Command.TipBalance)),
      cmd(s"${Command.TipWithdraw}")
        .text("EFL opnemen")
        .action((_, c) => c.copy(command = Command.TipWithdraw))
        .children(
          arg[String]("<adres>")
            .text("Het adres waar je naar wilt overmaken")
            .action((s, c) => c.copy(address = Some(s)))
            .validate(s => if (s.matches("^[LM3][a-km-zA-HJ-NP-Z1-9]{26,33}$")) success else failure("Dit lijkt niet op een EFL adres")),
          arg[Double]("<aantal>")
            .text("Aantal EFL die je wilt overmaken")
            .action((d, c) => c.copy(amount = Some(d)))
            .validate(d => if (d >= GlobalSettings.MIN_WITHDRAW_AMOUNT) success else failure(s"Dit moet minimaal ${GlobalSettings.MIN_WITHDRAW_AMOUNT} zijn"))
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
    val split = arguments
      .split(" ")
      .map(_.trim)
      .filter(_.nonEmpty)

    val args: Array[String] = split match {
      // Recombine a mention with several spaces into one argument
      case args if args.length > 2 &&
        args.headOption.contains("!tip") &&
        args.tail.headOption.exists(_.startsWith("@")) =>
        val mention = args.tail.filter(_.toDoubleOption.isEmpty).mkString(" ")
        val amount = args.last

        Array(args.head, mention, amount)

      // The first 2 arguments may make up a single command with a space. ("!tip adres" or "!tip balans")
      // Try to find a command from the first two arguments, if one exists the first two arguments are combined into one
      // string
      case args if args.length >= 2 &&
        Command.maybeWithName(args.take(2).mkString(" ")).nonEmpty =>
        Array(args.take(2).mkString(" ")) ++ args.tail.tail

      case args => args
    }

    parse(args)
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
