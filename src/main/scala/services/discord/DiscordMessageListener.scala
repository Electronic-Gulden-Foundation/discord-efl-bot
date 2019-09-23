package services.discord

import javax.inject.{Inject, Singleton}
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.{GenericEvent, ReadyEvent}
import net.dv8tion.jda.api.hooks.EventListener
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import services.discord.messagehandlers.{HelpMessageHandler, NoneMessageHandler}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DiscordMessageListener @Inject()(val jda: JDA,
                                       val lifecycle: ApplicationLifecycle,
                                       noneMessageHandler: NoneMessageHandler,
                                       helpMessageHandler: HelpMessageHandler)
                                      (implicit val ec: ExecutionContext)
  extends EventListener {

  private val logger = Logger(classOf[DiscordMessageListener])

  private val handlers = Seq(
    helpMessageHandler,
    noneMessageHandler
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

        if (e.getMessage.getContentDisplay.startsWith("!tip")) {
          this.handleTipBotMessage(e.getMessage)
        }

      case _ =>
        logger.debug(s"Unmatched event of type ${event.getClass}")
    }
  }

  def handleTipBotMessage(message: Message): Any = {
    val tipBotMessage = TipBotMessage(message)

    logger.debug(s"Handling message of type ${tipBotMessage.cmd.command}")

    val handled = handlers
      .filter(_.handles(tipBotMessage))
      .map(_.handleMessage(tipBotMessage))

    if (handled.isEmpty) {
      logger.debug(s"No handler found for message of type ${tipBotMessage.cmd.command}")
    }

    if (handled.isEmpty && tipBotMessage.cmd != TipBotCommand.None) {
      val text = s"""
           |Dat bericht heb ik helaas niet begrepen! Misschien heb je hier wat aan:
           |
           |${tipBotMessage.cmd.command.capitalize}:
           |${tipBotMessage.cmd.helpText}
           |""".stripMargin

      message.getChannel.sendMessage(text).queue()
    }
  }
}
