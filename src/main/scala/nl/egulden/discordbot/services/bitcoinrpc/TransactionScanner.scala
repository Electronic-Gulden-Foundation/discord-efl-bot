package nl.egulden.discordbot.services.bitcoinrpc

import akka.actor.ActorSystem
import javax.inject.{Inject, Named}
import nl.egulden.discordbot.services.SettingsService
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object TransactionScanner {
  val LAST_BLOCK_HASH_KEY = "transactionScanner-last_block_hash"
}

class TransactionScanner @Inject()(@Named("egulden") rpcClient: BitcoinRpcClient,
                                   actorSystem: ActorSystem,
                                   settingsService: SettingsService,
                                   transactionProcessor: WalletTransactionProcessor)
                                  (implicit ec: ExecutionContext) {

  private val logger = Logger(getClass)

  actorSystem.scheduler.schedule(5.seconds, 60.seconds) {

    for {
      lastBlockHash <- this.lastBlockHash()

      transactionsSinceBlock <- {
        logger.debug(s"Scanning transactions since block $lastBlockHash")

        rpcClient.listSinceBlock()
      }
      _ <- this.setLastBlockHash(transactionsSinceBlock.lastBlock())
    } yield {
      logger.debug(s"Found ${transactionsSinceBlock.transactions().size()} since ${lastBlockHash}")

      transactionsSinceBlock.transactions()
        .asScala
        .filter(_.category() == "receive")
        .map { transaction =>
          transactionProcessor.processTransaction(transaction.txId())
        }
    }
  }

  def lastBlockHash(): Future[Option[String]] =
    settingsService.get[String](TransactionScanner.LAST_BLOCK_HASH_KEY)

  def setLastBlockHash(value: String) =
    settingsService.set[String](TransactionScanner.LAST_BLOCK_HASH_KEY, value)
}
