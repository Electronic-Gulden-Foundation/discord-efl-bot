package services.discord

import net.dv8tion.jda.api.entities.Message
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec

class TipBotMessageTest extends PlaySpec with MockFactory {

  "TipBotCommand" should {
    "one must not equal the other" in {
      TipBotCommand.Tip == TipBotCommand.None mustBe false
      TipBotCommand.Tip.equals(TipBotCommand.None) mustBe false

      Seq(TipBotCommand.None).contains(TipBotCommand.Tip) mustBe false
    }

    "one must equal the same" in {
      TipBotCommand.Tip == TipBotCommand.Tip mustBe true
      TipBotCommand.Tip.equals(TipBotCommand.Tip) mustBe true

      Seq(TipBotCommand.Tip).contains(TipBotCommand.Tip) mustBe true
    }
  }

  "TipBotMessage" should {
    "parse messages without commands properly" in {
      TipBotMessage(msg("hallo dit is toch raar")).cmd mustBe TipBotCommand.None
      TipBotMessage(msg("dit is geen commando")).cmd mustBe TipBotCommand.None
    }

    "parse messages with commands properly" in {
      TipBotMessage(msg("!tip help wat is dit!")).cmd mustBe TipBotCommand.Help
      TipBotMessage(msg("!tip help wat is dit!")).args mustBe Seq("wat", "is", "dit!")

      TipBotMessage(msg("help wat is dit!")).cmd mustBe TipBotCommand.Help
      TipBotMessage(msg("help wat is dit!")).args mustBe Seq("wat", "is", "dit!")
    }

    "parse messages with whitespaces and enters properly" in {
      TipBotMessage(msg("help     wat    is      dit!")).args mustBe Seq("wat", "is", "dit!")
      TipBotMessage(msg("help   \n\n  wat   \n\n is     \n\n dit!")).args mustBe Seq.empty
    }

    "parse tips properly" in {
      TipBotMessage(msg("!tip @Henkie 0.12")).cmd mustBe TipBotCommand.Tip
      TipBotMessage(msg("@Henkie 0.12")).cmd mustBe TipBotCommand.Tip
    }

    "respect the minimum number of arguments" in {
      TipBotMessage(msg("!tip @Henkie")).isValid mustBe false
      TipBotMessage(msg("!tip @Henkie 1")).isValid mustBe true
    }
  }

  def msg(contents: String): Message = {
    val msg = stub[Message]
    (msg.getContentDisplay _).when().returns(contents)
    msg
  }
}
