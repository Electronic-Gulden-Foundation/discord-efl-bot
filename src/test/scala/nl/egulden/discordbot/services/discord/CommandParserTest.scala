package nl.egulden.discordbot.services.discord

import org.scalatestplus.play.PlaySpec

class CommandParserTest extends PlaySpec {

  "CommandParser" should {
    "usage should work" in {
      CommandParser.usage() must not be empty
    }

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

    "parse tip address command properly" in {
      CommandParser.parse("!tip adres") match {
        case Left(config) =>
          config.command mustBe Command.Tip
          config.subCommand mustBe Some(SubCommand.Address)

        case Right(byteArray) => fail(s"Must not fail to parse \n\n ${byteArray}")
      }
    }

    "parse balance command properly" in {
      CommandParser.parse("!tip balans") match {
        case Left(config) =>
          config.command mustBe Command.Tip
          config.subCommand mustBe Some(SubCommand.Balance)

        case Right(byteArray) => fail(s"Must not fail to parse \n\n ${byteArray}")
      }
    }

    "handle @mention with spaces in the name properly" in {
      CommandParser.parse("!tip @test user with a lot of spaces in the name 33") match {
        case Left(config) =>
          config.command mustBe Command.Tip
          config.name mustBe Some("@test user with a lot of spaces in the name")

        case Right(byteArray) =>
          fail(s"Must not fail to parse: \n\n ${byteArray}")
      }
    }
  }
}
