package nl.egulden.discordbot.services.discord

import org.scalatestplus.play.PlaySpec

class CommandParserTest extends PlaySpec {

  "CommandParser" should {
    "fail on an invalid command" in {
      CommandParser.parse("commandthatdoesnotexist") match {
        case Left(e) => fail("commandthatdoesnotexist must not be a valid command")
        case Right(byteArray) => println(byteArray.toString)
      }
    }

    "parse help command properly" in {
      CommandParser.parse("!help").left.toOption must not be empty
      CommandParser.parse("!help").left.toOption must not be empty

      CommandParser.parse("!help").left.get.command mustBe Command.Help
      CommandParser.parse("!help").left.get.subCommand mustBe empty
    }

    "parse ticker command properly" in {
      CommandParser.parse("!ticker").left.toOption must not be empty
      CommandParser.parse("!ticker").left.get.command mustBe Command.Ticker
    }

    "parse mining command properly" in {
      CommandParser.parse("!mining").left.toOption must not be empty
      CommandParser.parse("!mining").left.get.command mustBe Command.Mining
    }

    "parse tip command properly" in {
      CommandParser.parse("!tip @gebruiker 0.1") match {
        case Left(config) =>
          config.command mustBe Command.Tip
          config.subCommand mustBe empty
          config.name mustBe Some("@gebruiker")
          config.amount mustBe Some(0.1d)

        case _ =>
          fail("Must not fail")
      }
    }

    "fail to parse a tip command with missing username" in {
      CommandParser.parse("!tip") match {
        case Left(config) => fail("Must fail to parse")
        case _ => succeed
      }
    }

    "fail to parse a tip command with missing amount" in {
      CommandParser.parse("!tip @username") match {
        case Left(config) => fail("Must fail to parse")
        case _ => succeed
      }
    }

    "fail to parse a tip command with too low an amount" in {
      CommandParser.parse("!tip @username 0.000001") in {
        case Left(config) => fail("Must fail to parse")
        case _ => succeed
      }
    }

    "fail to parse a tip command that does not reference a member" in {
      CommandParser.parse("!tip username") match {
        case Left(config) => fail("Must fail to parse")
        case _ => succeed
      }
    }

    "parse tip address command properly" in {
      CommandParser.parse("!tip address") match {
        case Left(config) =>
          config.command mustBe Command.Tip
          config.subCommand mustBe Some(SubCommand.Address)

        case Right(byteArray) => fail(s"Must not fail to parse \n\n ${byteArray}")
      }
    }

    "parse balance command properly" in {
      CommandParser.parse("!tip balance") match {
        case Left(config) =>
          config.command mustBe Command.Tip
          config.subCommand mustBe Some(SubCommand.Balance)

        case Right(byteArray) => fail(s"Must not fail to parse \n\n ${byteArray}")
      }
    }
  }
}
