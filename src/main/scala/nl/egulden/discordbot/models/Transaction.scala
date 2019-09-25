package nl.egulden.discordbot.models

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import nl.egulden.discordbot.models.TransactionStatus.TransactionStatus
import nl.egulden.discordbot.models.TransactionType.TransactionType
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object TransactionStatus extends Enumeration {
  type TransactionStatus = Value

  val Confirmed = Value("confirmed")
  val Pending = Value("pending")
  val Invalid = Value("invalid")
}

object TransactionType extends Enumeration {
  type TransactionType = Value

  val Deposit = Value("deposit")
  val Withdrawal = Value("withdrawal")
  val Tip = Value("tip")
}

case class Transaction(id: Option[Long] = None,
                       fromUserId: Option[Long] = None,
                       toUserId: Option[Long] = None,
                       status: TransactionStatus,
                       transactionType: TransactionType,
                       amount: Long,
                       transactionId: Option[String] = None,
                       created: LocalDateTime = LocalDateTime.now())

object TransactionsTable {
  implicit val transactionStatusResultMapper = MappedColumnType.base[TransactionStatus, String](
    _.toString,
    TransactionStatus.withName
  )

  implicit val transactionTypeResultMapper = MappedColumnType.base[TransactionType, String](
    _.toString,
    TransactionType.withName
  )
}

class TransactionsTable(tag: Tag) extends Table[Transaction](tag, "transactions") {
  import TransactionsTable._

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def fromUserId = column[Option[Long]]("from_user_id")
  def toUserId = column[Option[Long]]("to_user_id")
  def status = column[TransactionStatus]("status")
  def transactionType = column[TransactionType]("transaction_type")
  def amount = column[Long]("amount")
  def transactionId = column[Option[String]]("transaction_id", O.Unique)
  def created = column[LocalDateTime]("created")

  def * = (
    id,
    fromUserId,
    toUserId,
    status,
    transactionType,
    amount,
    transactionId,
    created
  ) <> (Transaction.tupled, Transaction.unapply)
}

class TransactionsDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  val Transactions = TableQuery[TransactionsTable]

  def insert(transaction: Transaction)(implicit ec: ExecutionContext): Future[Transaction] =
    db.run((Transactions returning Transactions.map(_.id)) += transaction)
      .map(id => transaction.copy(id = id))

  def update(transaction: Transaction)(implicit ec: ExecutionContext): Future[Transaction] =
    db.run(Transactions
      .withFilter(_.id === transaction.id)
      .update(transaction.copy(id = transaction.id))
      .map(_ => transaction))
}
