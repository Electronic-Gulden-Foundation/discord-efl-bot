package nl.egulden.discordbot.services.discord.messagehandlers

import javax.inject.Inject
import nl.egulden.discordbot.GlobalSettings
import nl.egulden.discordbot.models.User
import nl.egulden.discordbot.services.bitcoinrpc.WalletAddressService
import nl.egulden.discordbot.services.discord.Command.Command
import nl.egulden.discordbot.services.discord.{BotMessage, Command, DiscordMessageSender}
import nl.egulden.discordbot.services.models.UsersService
import nl.egulden.discordbot.services.tipping.{TipWalletService, TippingError, TippingService}
import nl.egulden.discordbot.utils.bitcoin.SatoshiBigDecimal._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class TipMessageHandler @Inject()(usersService: UsersService,
                                  tippingService: TippingService,
                                  tipWalletService: TipWalletService,
                                  walletAddressService: WalletAddressService,
                                  discordMessageSender: DiscordMessageSender)
                                 (implicit val ec: ExecutionContext)
  extends TipBotMessageHandler {

  // TODO: Split the various handlers off into separate classes
  override def handlesTypes: Seq[Command] = Command.TipCommands

  override def handleMessage(msg: BotMessage): Unit = {
    usersService.findOrCreateUserByDiscordId(msg.message.getAuthor.getIdLong)
        .map { author =>
          msg.config.command match {
            case Command.TipAddress =>
              handleAddressSubCommand(author, msg)

            case Command.TipBalance =>
              handleBalanceSubCommand(author, msg)

            case Command.TipWithdraw =>
              handleWithdrawSubCommand(author, msg)

            case Command.Tip =>
              handleTip(author, msg)

            case command =>
              logger.debug(s"Unmatched command $command")
          }
        }
  }

  def handleAddressSubCommand(author: User, msg: BotMessage): Unit = {
    logger.debug("Handling address subcommand")

    walletAddressService.getOrCreateAddressFor(author)
      .onComplete {
        case Success(address) =>
          val addressStr = address.address
          logger.debug(s"Found address ${addressStr}, opening private channel...")

          msg.message.getAuthor.openPrivateChannel().queue(channel => {
            logger.debug("Sending address information")

            discordMessageSender.sendInChannel(channel, s"Je adres is $addressStr. Let op dat deze op elk moment kan wijzigen!")
            discordMessageSender.sendAddressQrCode(channel, addressStr)
          })

        case Failure(e) =>
          logger.error("Failed to get or create address", e)
      }
  }

  def handleBalanceSubCommand(author: User, msg: BotMessage): Unit = {
    logger.debug("Handling balance subcommand")

    tipWalletService.getBalance(author).map {
      case balance if balance == 0 =>
        discordMessageSender.pmToAuthor(msg.message, s"Je hebt helemaal geen EFL bij mij staan :slight_frown:")
        handleAddressSubCommand(author, msg)

      case balance =>
        discordMessageSender.pmToAuthor(msg.message, s"Je huidige balans is ${balance.satoshis} EFL")
    }
  }

  def handleTip(author: User, msg: BotMessage): Unit = {
    val mentioned = msg.message.getMentionedUsers

    if (mentioned.isEmpty) {
      discordMessageSender.replyToMessage(msg.message, "geen @mention gevonden in je bericht :(")
    } else if (msg.config.amount.isEmpty || msg.config.amount.exists(_ < GlobalSettings.MIN_TIP_AMOUNT)) {
      discordMessageSender.replyToMessage(msg.message, s"je moet minimaal ${GlobalSettings.MIN_TIP_AMOUNT} EFL tippen")
    } else if (mentioned.size() > 1) {
      discordMessageSender.replyToMessage(msg.message, "teveel @mentions gevonden in je bericht, probeer het nog eens met maar 1 mention")
    } else {
      val tippedDiscordUser = mentioned.get(0)

      if (msg.message.getAuthor == tippedDiscordUser) {
        discordMessageSender.replyToMessage(msg.message, "je bent natuurlijk geweldig, maar je kunt jezelf geen tip geven")
      } else if (tippedDiscordUser == msg.message.getJDA.getSelfUser) {
        discordMessageSender.replyToMessage(msg.message, "helaas mag ik geen tips ontvangen van mijn maker :frowning:")
      } else {
        for {
          tippedUser <- usersService.findOrCreateUserByDiscordId(tippedDiscordUser.getIdLong)
          transactionOrError <- tippingService.tip(author, tippedUser, msg.config.amount.get)
        } yield {
          transactionOrError match {
            case Left(transaction) =>
              discordMessageSender.replyToMessage(msg.message, s"Je hebt ${tippedDiscordUser.getAsMention} ${msg.config.amount.get} EFL getipt!")

            case Right(error) if (error == TippingError.NotEnoughBalance) =>
              discordMessageSender.replyToMessage(msg.message, "Je hebt niet genoeg balans :frowning:")
          }
        }
      }
    }
  }

  def handleWithdrawSubCommand(author: User, msg: BotMessage): Unit = {
    tipWalletService.withdraw(author, msg.config.address.get, msg.config.amount.get)
      .onComplete {
        case Success(Some(transaction)) =>
          discordMessageSender.pmToAuthor(
            msg.message,
            "Je transactie wordt binnenkort verzonden! Deze wordt eerst handmatig gecontroleerd dus dat kan even duren."
          )

          discordMessageSender.sendToAdmin(
            s"Gebruiker ${msg.message.getAuthor.getName} heeft een withdrawal opgevraagd " +
              s"egulden-cli sendtoaddress ${msg.config.address.get} ${msg.config.amount.get}"
          )

        case Success(None) =>
          tipWalletService.getBalance(author)
            .map { balance =>
              discordMessageSender.pmToAuthor(msg.message, s"Helaas je hebt niet genoeg balans :frowning: Je hebt nu $balance EFL")
            }

        case Failure(e) =>
          logger.error("Withdraw subcommand failed", e)
      }
  }
}
