package nl.egulden.discordbot.services.discord.messagehandlers
import java.io.File

import com.google.zxing.EncodeHintType
import javax.inject.Inject
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.{Message, MessageEmbed}
import net.glxn.qrgen.core.image.ImageType
import net.glxn.qrgen.javase.QRCode
import nl.egulden.discordbot.services.discord.DiscordMessageSending
import play.api.{Configuration, Logger}

class NoCommandMessageHandler @Inject()(configuration: Configuration) extends DiscordMessageSending {
  private val logger = Logger(getClass)

  def addressLinkTemplate: String = configuration.get[String]("app.linktemplates.address")
  def transactionLinkTemplate: String = configuration.get[String]("app.linktemplates.transaction")

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

      sendInChannel(message, String.format(transactionLinkTemplate, word))
    }
  }

  def checkAddress(message: Message, word: String): Unit = {
    if (word.matches("^[LM3][a-km-zA-HJ-NP-Z1-9]{26,33}$")) {
      logger.debug(s"Found address ${word.substring(0, 12)}...")

      val file = QRCode
        .from(s"egulden:${word}")
        .withSize(256, 256)
        .withHint(EncodeHintType.MARGIN, "1")
        .to(ImageType.JPG)
        .file()
      val filename = s"$word.jpg"

      message.getChannel.sendFile(file, filename)
          .embed(new EmbedBuilder()
            .setImage(s"attachment://$filename")
            .setDescription(String.format(addressLinkTemplate, word))
            .build())
          .queue()
    }
  }
}
