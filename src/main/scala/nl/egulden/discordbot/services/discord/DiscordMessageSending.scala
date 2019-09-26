package nl.egulden.discordbot.services.discord

import net.dv8tion.jda.api.entities.{Message, User => DiscordUser}

trait DiscordMessageSending {

  def sendInChannel(msg: Message, text: String): Unit =
    msg.getChannel.sendMessage(text).queue()

  def replyToMessage(msg: Message, reply: String): Unit =
    this.sendInChannel(msg, s"${msg.getAuthor.getAsMention} $reply")

  def pmToAuthor(msg: Message, text: String): Unit =
    pmToUser(msg.getAuthor, text)

  def pmToUser(discordUser: DiscordUser, text: String): Unit =
    discordUser.openPrivateChannel().queue(channel => {
      channel.sendMessage(text).queue()
    })

}
