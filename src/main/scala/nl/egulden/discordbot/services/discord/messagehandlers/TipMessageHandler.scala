package nl.egulden.discordbot.services.discord.messagehandlers

import javax.inject.Inject
import nl.egulden.discordbot.GlobalSettings
import nl.egulden.discordbot.models.User
import nl.egulden.discordbot.services.bitcoinrpc.WalletAddressService
import nl.egulden.discordbot.services.discord.Command.Command
import nl.egulden.discordbot.services.discord.{BotMessage, Command, DiscordMessageSending, SubCommand}
import nl.egulden.discordbot.services.models.UsersService
import nl.egulden.discordbot.services.tipping.{TipWalletService, TippingError, TippingService}
import nl.egulden.discordbot.utils.bitcoin.SatoshiBigDecimal._

import scala.concurrent.ExecutionContext

class TipMessageHandler @Inject()(usersService: UsersService,
                                  tippingService: TippingService,
                                  tipWalletService: TipWalletService,
                                  walletAddressService: WalletAddressService)
                                 (implicit val ec: ExecutionContext)
  extends TipBotMessageHandler
    with DiscordMessageSending {

  override def handlesTypes: Seq[Command] = Seq(Command.Tip)

  override def handleMessage(msg: BotMessage): Unit = {
    usersService.findOrCreateUserByDiscordId(msg.message.getAuthor.getIdLong)
        .map { author =>
          msg.config.subCommand match {
            case Some(SubCommand.Address) =>
              handleAddressSubCommand(author, msg)

            case Some(SubCommand.Balance) =>
              handleBalanceSubCommand(author, msg)

            case None =>
              handleTip(author, msg)

            case Some(subCommand) =>
              logger.debug(s"Unmatched subcommand $subCommand")
          }
        }
  }

  def handleAddressSubCommand(author: User, msg: BotMessage): Unit = {
    walletAddressService.getOrCreateAddressFor(author)
      .map { address =>
        this.pmToAuthor(msg.message, s"Je adres is ${address.address}. Let op dat deze op elk moment kan wijzigen!")
      }
  }

  def handleBalanceSubCommand(author: User, msg: BotMessage): Unit = {
    tipWalletService.getBalance(author).map {
      case balance if balance == 0 =>
        this.pmToAuthor(msg.message, s"Je hebt helemaal geen EFL bij mij staan :(")

        handleAddressSubCommand(author, msg)

      case balance =>
        this.pmToAuthor(msg.message, s"Je huidige balans is ${balance.satoshis} EFL")
    }
  }

  def handleTip(author: User, msg: BotMessage): Unit = {
    val mentioned = msg.message.getMentionedUsers

    if (mentioned.isEmpty) {
      this.replyToMessage(msg.message, "geen @mention gevonden in je bericht :(")
    } else if (msg.config.amount.isEmpty || msg.config.amount.exists(_ < GlobalSettings.MIN_TIP_AMOUNT)) {
      this.replyToMessage(msg.message, s"je moet minimaal ${GlobalSettings.MIN_TIP_AMOUNT} EFL tippen")
    } else if (mentioned.size() > 1) {
      this.replyToMessage(msg.message, "teveel @mentions gevonden in je bericht, probeer het nog eens met maar 1 mention")
    } else {
      val tippedDiscordUser = mentioned.get(0)

      if (msg.message.getAuthor == tippedDiscordUser) {
        this.replyToMessage(msg.message, "je bent natuurlijk geweldig, maar je kunt jezelf geen tip geven")
      } else if (tippedDiscordUser == msg.message.getJDA.getSelfUser) {
        this.replyToMessage(msg.message, "helaas mag ik geen tips ontvangen van mijn maker :(")
      } else {
        for {
          tippedUser <- usersService.findOrCreateUserByDiscordId(tippedDiscordUser.getIdLong)
          transactionOrError <- tippingService.tip(author, tippedUser, msg.config.amount.get)
        } yield {
          transactionOrError match {
            case Left(transaction) =>
              replyToMessage(msg.message, s"Je hebt ${tippedDiscordUser.getAsMention} ${msg.config.amount.get} EFL getipt!")
              pmToUser(tippedDiscordUser, s"Je hebt ${msg.config.amount.get} EFL gekregen van ${msg.message.getAuthor.getAsMention}")

            case Right(error) if (error == TippingError.NotEnoughBalance) =>
              replyToMessage(msg.message, "Je hebt niet genoeg balans :(")
          }
        }
      }
    }
  }
}
