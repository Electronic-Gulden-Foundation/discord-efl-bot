package nl.egulden.discordbot.services.discord

import akka.actor.{Actor, ActorSystem, Props}
import javax.inject.{Inject, Singleton}
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.{ User => DiscordUser }
import nl.egulden.discordbot.models.{Transaction, TransactionStatus, User, UsersDAO}
import nl.egulden.discordbot.services.akka.{TransactionCreatedEvent, TransactionUpdatedEvent}
import play.api.Logger
import nl.egulden.discordbot.utils.bitcoin.SatoshiBigDecimal._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransactionEventListener @Inject()(actorSystem: ActorSystem,
                                         usersDAO: UsersDAO,
                                         jda: JDA)
                                        (implicit ec: ExecutionContext)
    extends DiscordMessageSending {

  startup()

  def startup() = {
    actorSystem.actorOf(
      TransactionEventListenerActor.props(usersDAO, jda),
      "transaction-event-listener"
    )
  }
}

object TransactionEventListenerActor {
  def props(usersDAO: UsersDAO, jda: JDA)(implicit ec: ExecutionContext) =
    Props(classOf[TransactionEventListenerActor], usersDAO, jda, ec)
}

class TransactionEventListenerActor(usersDAO: UsersDAO,
                                    jda: JDA)
                                   (implicit ec: ExecutionContext)
  extends Actor
    with DiscordMessageSending {

  private val logger = Logger(getClass)

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[TransactionCreatedEvent])
    context.system.eventStream.subscribe(self, classOf[TransactionUpdatedEvent])
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self, classOf[TransactionCreatedEvent])
    context.system.eventStream.unsubscribe(self, classOf[TransactionUpdatedEvent])
  }

  override def receive: Receive = {
    case event: TransactionCreatedEvent =>
      logger.debug(s"Received ${event.getClass}")

      if (event.transaction.status == TransactionStatus.Confirmed) {
        notifyUserOfTransactionConfirmed(event.transaction)
      } else {
        notifyUserOfTransactionReceived(event.transaction)
      }

    case event: TransactionUpdatedEvent =>
      logger.debug(s"Received ${event.getClass}")

      if (event.transaction.status == TransactionStatus.Confirmed) {
        notifyUserOfTransactionConfirmed(event.transaction)
      }
  }

  def notifyUserOfTransactionReceived(transaction: Transaction)
                                     (implicit ec: ExecutionContext): Future[Unit] =
    this.getUserForTransaction(transaction)
      .map { case (user, discordUser) =>
        this.pmToUser(discordUser, s"Ik heb een transactie van jou ontvangen! Je ontvangt straks ${transaction.amount.satoshi} EFL")
      }

  def notifyUserOfTransactionConfirmed(transaction: Transaction): Future[Unit] =
    this.getUserForTransaction(transaction)
      .map { case (user, discordUser) =>
        this.pmToUser(discordUser, s"Je transactie is bevestigd! Je hebt nu ${transaction.amount.satoshi} erbij!")
      }

  def getUserForTransaction(transaction: Transaction): Future[(User, DiscordUser)] =
    usersDAO.byId(transaction.toUserId.get)
      .map(_.get)
      .map(user => (user, jda.getUserById(user.discordUserId)))
}
