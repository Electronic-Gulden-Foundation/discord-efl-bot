package nl.egulden.discordbot.services.tipping

import javax.inject.Inject
import nl.egulden.discordbot.models.{Transaction, TransactionStatus, TransactionType, TransactionsDAO, TransactionsTable, User}
import nl.egulden.discordbot.utils.bitcoin.SatoshiBigDecimal._

import scala.concurrent.{ExecutionContext, Future}

class TipWalletService @Inject()(transactionsDAO: TransactionsDAO) {

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
}
