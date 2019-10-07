package nl.egulden.discordbot.services.discord

import com.google.zxing.EncodeHintType
import javax.inject.Inject
import net.dv8tion.jda.api.{EmbedBuilder, JDA}
import net.dv8tion.jda.api.entities.{Message, MessageChannel, User => DiscordUser}
import net.glxn.qrgen.core.image.ImageType
import net.glxn.qrgen.javase.QRCode
import play.api.{Configuration, Logger}

class DiscordMessageSender @Inject()(configuration: Configuration,
                                     jda: JDA) {

  val logger = Logger(getClass)

  def addressLinkTemplate: String = configuration.get[String]("app.linktemplates.address")
  def adminUserId: Long = configuration.get[Long]("discord.adminUserId")
  def transactionLinkTemplate: String = configuration.get[String]("app.linktemplates.transaction")

  def getAdminUser: DiscordUser = jda.getUserById(adminUserId)

  def sendToAdmin(text: String): Unit = pmToUser(getAdminUser, text)

  def sendInChannel(msg: Message, text: String): Unit =
    sendInChannel(msg.getChannel, text)

  def sendInChannel(channel: MessageChannel, text: String): Unit =
    channel.sendMessage(text).queue()

  def replyToMessage(msg: Message, reply: String): Unit =
    this.sendInChannel(msg, s"${msg.getAuthor.getAsMention} $reply")

  def pmToAuthor(msg: Message, text: String): Unit =
    pmToUser(msg.getAuthor, text)

  def pmToUser(discordUser: DiscordUser, text: String): Unit =
    discordUser.openPrivateChannel().queue(channel => {
      channel.sendMessage(text).queue()
    })

  def sendAddressQrCode(channel: MessageChannel, address: String): Unit = {
    val filename = s"$address.jpg"
    val url = s"egulden:$address"
    val file = QRCode
      .from(url)
      .withSize(256, 256)
      .withHint(EncodeHintType.MARGIN, "1")
      .to(ImageType.JPG)
      .file()

    channel.sendFile(file, filename)
      .embed(new EmbedBuilder()
        .setImage(s"attachment://$filename")
        .setDescription(String.format(addressLinkTemplate, address))
        .build())
      .queue()
  }
}
