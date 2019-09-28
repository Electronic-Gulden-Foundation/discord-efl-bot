package nl.egulden.discordbot.services.akka

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import nl.egulden.discordbot.models.Transaction
import play.api.Logger

trait Event

case class TransactionCreatedEvent(transaction: Transaction) extends Event
case class TransactionUpdatedEvent(transaction: Transaction, oldTransaction: Transaction) extends Event

/**
 * Publishes messages on the ActorSystem, so other classes don't need to depend on it
 */
class EventPublisher @Inject()(actorSystem: ActorSystem) {

  private val logger = Logger(getClass)

  def publish(event: Event): Unit = {
    logger.info(s"Published event: ${event.getClass}")

    actorSystem.eventStream.publish(event)
  }
}
