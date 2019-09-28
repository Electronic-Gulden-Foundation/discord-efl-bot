package nl.egulden.discordbot.services.bitcoinrpc

import javax.inject.{Inject, Named, Singleton}
import nl.egulden.discordbot.GlobalSettings
import nl.egulden.discordbot.models.TransactionStatus.TransactionStatus
import nl.egulden.discordbot.models._
import nl.egulden.discordbot.services.akka.{EventPublisher, TransactionCreatedEvent, TransactionUpdatedEvent}
import nl.egulden.discordbot.services.tipping.TipWalletService
import nl.egulden.discordbot.utils.bitcoin.SatoshiBigDecimal._
import play.api.Logger
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

@Singleton
class WalletTransactionProcessor @Inject()(@Named("egulden") rpcClient: BitcoinRpcClient,
                                           transactionsDAO: TransactionsDAO,
                                           walletAddressesDAO: WalletAddressesDAO,
                                           tipWalletService: TipWalletService,
                                           eventPublisher: EventPublisher)
                                          (implicit ec: ExecutionContext) {

  val logger = Logger(getClass)

  /**
   * Process a single transaction with given txId
   *
   * @param txId
   * @return
   */
  def processTransaction(txId: String): Future[Seq[Transaction]] = {
    for {
      rpcTransaction <- rpcClient.getRawTransaction(txId)

      // confirmations is null when there are no confirmations
      txConfirmations <- Future(Option(rpcTransaction.confirmations()).map(_.toInt).getOrElse(0))

      isConfirmed <- Future(txConfirmations >= GlobalSettings.MIN_CONFIRMATIONS)

      status <- Future(if (isConfirmed) TransactionStatus.Confirmed else TransactionStatus.Pending)

      transactions <- processTransactionOutputs(rpcTransaction, status)
    } yield transactions
  }

  /**
   * Processes the outputs of the given transaction
   *
   * @param rpcTransaction
   * @param status
   * @return
   */
  private def processTransactionOutputs(rpcTransaction: RawTransaction, status: TransactionStatus): Future[Seq[Transaction]] = {
    for {
      // First find all addresses for all vouts
      userAddresses <- mapVoutsWithAddresses(rpcTransaction.vOut().asScala.toSeq)

      // Now handle every address + vout combination
      userTransactions <- Future.sequence {
        userAddresses.map { case (vout, userAddress) =>
          processTransactionOutput(vout, userAddress, status)
        }
      }
    } yield userTransactions
  }

  /**
   * Processes a single vout
   *
   * @param vout
   * @param userAddress
   * @param status
   * @return
   */
  private def processTransactionOutput(vout: RawTransaction.Out, userAddress: WalletAddress, status: TransactionStatus): Future[Transaction] =
    for {
      maybeExistingUserTransaction <- transactionsDAO.findByTransactionIdAndVout(vout.transaction().txId(), vout.n())

      updatedUserAddress <- tipWalletService.updateAddressUsage(userAddress)

      userTransaction <- maybeExistingUserTransaction match {
        case Some(userTransaction) =>
          logger.debug(s"Found existing tx for ${vout.transaction().txId()}:${vout.n()}")

          val oldTransaction = userTransaction
          val newTransaction = userTransaction.copy(
            status = status,
          )

          val hasChanged = newTransaction.productIterator.toList != oldTransaction.productIterator.toList

          transactionsDAO.update(newTransaction)
            .map { tx =>
              if (hasChanged) {
                eventPublisher.publish(TransactionUpdatedEvent(tx, userTransaction))
              }

              tx
            }

        case None =>
          logger.debug(s"Creating new tx for ${vout.transaction().txId()}:${vout.n()}")

          transactionsDAO.insert(Transaction(
            toUserId = Some(updatedUserAddress.currentUserId),
            amount = BigDecimal(vout.value()).toSatoshi,
            transactionType = TransactionType.Deposit,
            status = status,
            transactionId = Some(vout.transaction().txId()),
            vout = Some(vout.n())
          )).map { tx =>
            eventPublisher.publish(TransactionCreatedEvent(tx))
            tx
          }
      }
    } yield userTransaction

  /**
   * Matches all given vouts with stored addresses. Filters vouts that do not have an address.
   *
   * @param vouts
   * @return
   */
  private def mapVoutsWithAddresses(vouts: Seq[RawTransaction.Out]): Future[Seq[(RawTransaction.Out, WalletAddress)]] =
    for {
      unfiltered <- Future.sequence {
        vouts

          // More than one address in the vout means that it is a multisig transaction
          // it would be way too complex to do something sensible with this without risk
          // much better to simply ignore this vout altogether. Let the user figure it out
          .filter(_.scriptPubKey().addresses().size() == 1)

          // Try to find address for this vout
          .map { vout =>
            val address = vout.scriptPubKey().addresses().get(0)

            walletAddressesDAO.byAddress(address)
              .map((vout, _))
          }
      }

    } yield {
      // Filter all entries without an address
      unfiltered.filter(_._2.isDefined).map(r => (r._1, r._2.get))
    }
}
