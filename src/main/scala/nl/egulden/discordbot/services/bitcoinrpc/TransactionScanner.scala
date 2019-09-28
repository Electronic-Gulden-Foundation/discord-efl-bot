package nl.egulden.discordbot.services.bitcoinrpc

import akka.actor.ActorSystem
import javax.inject.{Inject, Named}
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class TransactionScanner @Inject()(@Named("egulden") rpcClient: BitcoinRpcClient,
                                   actorSystem: ActorSystem,
                                   transactionProcessor: WalletTransactionProcessor)
                                  (implicit ec: ExecutionContext) {

  // TODO: Store this in a database
  private var lastBlockHash: Option[String] = None

  private val logger = Logger(getClass)

  actorSystem.scheduler.schedule(5.seconds, 60.seconds) {
    logger.debug(s"Scanning transactions since block ${lastBlockHash}")

    rpcClient.listSinceBlock(lastBlockHash)
      .map { transactionsSinceBlock =>
        lastBlockHash = Some(transactionsSinceBlock.lastBlock())

        logger.debug(s"Found ${transactionsSinceBlock.transactions().size()} since ${lastBlockHash}")

        transactionsSinceBlock.transactions()
          .asScala
          .filter(_.category() == "receive")
          .map { transaction =>
            transactionProcessor.processTransaction(transaction.txId())
          }
      }
  }
}
