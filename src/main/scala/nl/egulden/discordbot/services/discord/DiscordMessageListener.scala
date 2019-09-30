package nl.egulden.discordbot.services.discord

import com.typesafe.config.ConfigList
import javax.inject.{Inject, Singleton}
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.{ChannelType, Message, MessageType}
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.events.{GenericEvent, ReadyEvent}
import net.dv8tion.jda.api.hooks.EventListener
import nl.egulden.discordbot.services.discord.messagehandlers.{CryptoWordFinderMessageHandler, HelpMessageHandler, MiningMessageHandler, TickerMessageHandler, TipMessageHandler}
import play.api.{Configuration, Logger}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

// TODO: Create blacklist / whitelist channels

@Singleton
class DiscordMessageListener @Inject()(val jda: JDA,
                                       val lifecycle: ApplicationLifecycle,
                                       helpMessageHandler: HelpMessageHandler,
                                       miningMessageHandler: MiningMessageHandler,
                                       tickerMessageHandler: TickerMessageHandler,
                                       tipMessageHandler: TipMessageHandler,
                                       cryptoWordFinderMessageHandler: CryptoWordFinderMessageHandler,
                                       configuration: Configuration)
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
      case e: MessageReceivedEvent if shouldNotHandleMessageEvent(e) =>
        // Do nothing

      case e: MessageReceivedEvent =>
        cryptoWordFinderMessageHandler.handleMessage(e.getMessage)

      case _ =>
        // Do nothing
    }

    event match {
      case e: ReadyEvent =>
        logger.debug(s"Connected to discord guilds: ${e.getGuildTotalCount}")

      case e: MessageReceivedEvent if shouldNotHandleMessageEvent(e) =>
        logger.debug(s"Ignoring message")

      case e: MessageReceivedEvent if e.getAuthor != jda.getSelfUser && e.getMessage.getChannelType != ChannelType.PRIVATE =>
        logger.debug(s"Received message ${e.getMessage.getContentDisplay}")

        if (e.getMessage.getContentDisplay.startsWith("!")) {
          this.handleBotMessage(e.getMessage)
        }

      case e: PrivateMessageReceivedEvent if e.getAuthor != jda.getSelfUser  =>
        logger.debug(s"Received private message: ${e.getMessage.getContentDisplay}")

        this.handleBotMessage(e.getMessage)

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

  def shouldNotHandleMessageEvent(event: MessageReceivedEvent): Boolean = {
    val whitelistChannelId = configuration.get[Long]("discord.whitelistChannelId")

    whitelistChannelId != -1L && whitelistChannelId != event.getMessage.getChannel.getIdLong
  }
}
