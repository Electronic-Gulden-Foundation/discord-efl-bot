package nl.egulden.discordbot.services.discord.messagehandlers
import javax.inject.Inject
import nl.egulden.discordbot.services.discord.Command.Command
import nl.egulden.discordbot.services.discord.{BotMessage, Command, CommandParser}
import nl.egulden.discordbot.services.models.UsersService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class HelpMessageHandler @Inject()(usersService: UsersService)
                                  (implicit ec: ExecutionContext)
  extends TipBotMessageHandler {

  override def handlesTypes: Seq[Command] = Seq(Command.Help)

  override def handleMessage(msg: BotMessage): Unit = {
    Await.result(usersService.findOrCreateUserByDiscordId(msg.message.getAuthor.getIdLong), Duration.Inf)

    msg.message.getChannel.sendMessage(CommandParser.usage()).queue()
  }
}
