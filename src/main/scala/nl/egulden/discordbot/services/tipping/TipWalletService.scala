package nl.egulden.discordbot.services.tipping

import javax.inject.Inject
import nl.egulden.discordbot.models._
import nl.egulden.discordbot.services.bitcoinrpc.WalletAddressService
import nl.egulden.discordbot.utils.bitcoin.SatoshiBigDecimal._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

class TipWalletService @Inject()(transactionsDAO: TransactionsDAO,
                                 usersDAO: UsersDAO,
                                 walletAddressService: WalletAddressService) {

  def hasBalance(user: User, amount: Double)(implicit ec: ExecutionContext): Future[Boolean] =
    getBalance(user).map(_ >= amount.toSatoshi)

  def getBalance(user: User)(implicit ec: ExecutionContext): Future[Long] =
    transactionsDAO.sumUserBalance(user)

  def createTipTransaction(tippingUser: User, tippedUser: User, amount: Double)(implicit ec: ExecutionContext): Future[Transaction] =
    transactionsDAO.insert(Transaction(
      fromUserId = tippingUser.id,
      toUserId = tippedUser.id,
      status = TransactionStatus.Confirmed,
      transactionType = TransactionType.Tip,
      amount = amount.toSatoshi,
    ))

  def updateAddressUsage(walletAddress: WalletAddress)
                        (implicit ec: ExecutionContext): Future[WalletAddress] =
    updateAddressUsage(walletAddress, walletAddress.currentUserId)

  def updateAddressUsage(walletAddress: WalletAddress,
                         currentUserId: Long)
                        (implicit ec: ExecutionContext): Future[WalletAddress] =
    walletAddressService.updateAddressUsage(walletAddress, currentUserId)

}
