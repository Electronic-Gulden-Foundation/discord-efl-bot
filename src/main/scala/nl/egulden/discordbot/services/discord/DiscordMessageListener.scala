package nl.egulden.discordbot.services.discord

import javax.inject.{Inject, Singleton}
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.{GenericEvent, ReadyEvent}
import net.dv8tion.jda.api.hooks.EventListener
import nl.egulden.discordbot.services.discord.messagehandlers.HelpMessageHandler
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DiscordMessageListener @Inject()(val jda: JDA,
                                       val lifecycle: ApplicationLifecycle,
                                       helpMessageHandler: HelpMessageHandler)
                                      (implicit val ec: ExecutionContext)
  extends EventListener {

  private val logger = Logger(classOf[DiscordMessageListener])

  private val handlers = Seq(
    helpMessageHandler,
  )

  startup()

  def startup(): Unit = {
    jda.addEventListener(this)

    lifecycle.addStopHook { () =>
      Future {
        jda.removeEventListener(this)
      }
    }
  }

  override def onEvent(event: GenericEvent): Unit = {
    event match {
      case e: ReadyEvent =>
        logger.debug(s"Connected to discord guilds: ${e.getGuildTotalCount}")

      case e: MessageReceivedEvent if (e.getAuthor != jda.getSelfUser) =>
        logger.debug(s"Received message ${e.getMessage.getContentDisplay}")

        if (e.getMessage.getContentDisplay.startsWith("!") &&
          e.getMessage.getChannel.getId == "625649671959740417") {
          this.handleBotMessage(e.getMessage)
        }

      case _ =>
        logger.debug(s"Unmatched event of type ${event.getClass}")
    }
  }

  def handleBotMessage(message: Message): Any = {
    CommandParser.parse(message.getContentDisplay) match {
      case Left(config) =>
        val botMessage = BotMessage(message, config)

        logger.debug(s"Handling message of type ${botMessage.config.command}")

        val handled = handlers
          .filter(_.handles(botMessage))
          .map(_.handleMessage(botMessage))

        if (handled.isEmpty) {
          logger.debug(s"No handler found for message of type ${botMessage.config.command}")
        }

      case Right(byteArray) =>
        logger.debug(s"Failed to parse message:\n\n${byteArray}")

    }
  }
}
