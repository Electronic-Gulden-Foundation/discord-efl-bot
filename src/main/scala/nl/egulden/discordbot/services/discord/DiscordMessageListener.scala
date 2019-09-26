package nl.egulden.discordbot.services.discord

import javax.inject.{Inject, Singleton}
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.{ChannelType, Message}
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.events.{GenericEvent, ReadyEvent}
import net.dv8tion.jda.api.hooks.EventListener
import nl.egulden.discordbot.services.discord.messagehandlers.{HelpMessageHandler, MiningMessageHandler, TickerMessageHandler, TipMessageHandler}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DiscordMessageListener @Inject()(val jda: JDA,
                                       val lifecycle: ApplicationLifecycle,
                                       helpMessageHandler: HelpMessageHandler,
                                       miningMessageHandler: MiningMessageHandler,
                                       tickerMessageHandler: TickerMessageHandler,
                                       tipMessageHandler: TipMessageHandler)
                                      (implicit val ec: ExecutionContext)
  extends EventListener {

  private val logger = Logger(classOf[DiscordMessageListener])

  private val handlers = Seq(
    helpMessageHandler,
    miningMessageHandler,
    tickerMessageHandler,
    tipMessageHandler
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
          // TODO: Create blacklist / whitelist channels
          e.getMessage.getChannel.getId == "625649671959740417") {
          this.handleBotMessage(e.getMessage)
        }

      case e: PrivateMessageReceivedEvent if (e.getAuthor != jda.getSelfUser)  =>
        logger.debug(s"Received private message: ${e.getMessage.getContentDisplay}")

        this.handleBotMessage(e.getMessage) match {
          case false =>
            e.getChannel.sendMessage(CommandParser.usage()).queue()

          case _ =>
            // Do nothing
        }

      case _ =>
        logger.debug(s"Unmatched event of type ${event.getClass}")
    }
  }

  def handleBotMessage(message: Message): Boolean = {
    CommandParser.parse(message.getContentDisplay) match {
      case Left(config) =>
        val botMessage = BotMessage(message, config.copy(isPrivateMessage = message.isFromType(ChannelType.PRIVATE)))

        logger.debug(s"Handling message of type ${botMessage.config.command}")

        val handled = handlers
          .filter(_.handles(botMessage))
          .map(_.handleMessage(botMessage))

        if (handled.isEmpty) {
          logger.debug(s"No handler found for message of type ${botMessage.config.command}")
        }

        handled.nonEmpty

      case Right(byteArray) =>
        if (message.getContentDisplay.startsWith("!")) {
          message.getChannel.sendMessage(byteArray.toString)
        }

        logger.debug(s"Failed to parse message:\n ${byteArray.toString}")

        false
    }
  }
}
