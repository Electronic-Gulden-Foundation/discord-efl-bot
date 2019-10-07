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

          config.name mustBe Some("@gebruiker")
          config.amount mustBe Some(0.1d)

        case Right(str) =>
          fail(s"Must not fail to parse:\n\n$str")
      }
    }

    "parse tip address command properly" in {
      CommandParser.parse("!tip adres") match {
        case Left(config) =>
          config.command mustBe Command.TipAddress

        case Right(str) => fail(s"Must not fail to parse \n\n ${str}")
      }
    }

    "parse balance command properly" in {
      CommandParser.parse("!tip balans") match {
        case Left(config) =>
          config.command mustBe Command.TipBalance

        case Right(str) => fail(s"Must not fail to parse \n\n ${str}")
      }
    }

    "handle @mention with spaces in the name properly" in {
      CommandParser.parse("!tip @test user with a lot of spaces in the name 33") match {
        case Left(config) =>
          config.command mustBe Command.Tip
          config.name must contain("@test user with a lot of spaces in the name")

        case Right(str) =>
          fail(s"Must not fail to parse: \n\n ${str}")
      }
    }

    "parse withdraw command properly" in {
      CommandParser.parse("!tip opnemen LczjPQp1xnCAvnXNmLQnHj4L6xZj8HLKYy 10") match {
        case Left(config) =>
          println(config)

          config.command mustBe Command.TipWithdraw

          config.address must contain("LczjPQp1xnCAvnXNmLQnHj4L6xZj8HLKYy")
          config.amount must contain(10d)

        case Right(str) =>
          fail(s"Must not fail to parse:\n\n${str}")
      }
    }

    "fail to arse withdraw command with invalid address" in {
      CommandParser.parse("!tip opnemen wasdf 10") match {
        case Left(config) =>
          fail("Should fail to parse invalid address")

        case Right(_) =>
          succeed
      }
    }
  }
}
