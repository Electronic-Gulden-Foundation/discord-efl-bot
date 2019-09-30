package nl.egulden.discordbot.services.discord.messagehandlers
import javax.inject.Inject
import net.dv8tion.jda.api.entities.Message
import nl.egulden.discordbot.services.discord.DiscordMessageSender
import play.api.Logger


/**
 * Finds addresses, transaction ids, block ids etc in messages and outputs a link and/or qr code
 */
class CryptoWordFinderMessageHandler @Inject()(discordMessageSender: DiscordMessageSender)  {
  private val logger = Logger(getClass)

  def handleMessage(message: Message): Unit = {
    message.getContentDisplay
      .split(" ")
      .foreach(word => {
        checkTransactionId(message, word)
        checkAddress(message, word)
      })
  }

  def checkTransactionId(message: Message, word: String): Unit = {
    if (word.matches("^[A-Fa-f0-9]{64}$")) {
      logger.debug(s"Found transaction ${word.substring(0, 12)}...")

      discordMessageSender.sendInChannel(message, String.format(discordMessageSender.transactionLinkTemplate, word))
    }
  }

  def checkAddress(message: Message, word: String): Unit = {
    if (word.matches("^[LM3][a-km-zA-HJ-NP-Z1-9]{26,33}$")) {
      logger.debug(s"Found address ${word.substring(0, 12)}...")

      discordMessageSender.sendAddressQrCode(message.getChannel, word)
    }
  }
}
