package nl.egulden.discordbot.models

import com.github.tototoshi.slick.MySQLJodaSupport._
import javax.inject.Inject
import nl.egulden.discordbot.models.TransactionStatus.TransactionStatus
import nl.egulden.discordbot.models.TransactionType.TransactionType
import org.joda.time.DateTime
import play.api.Logger
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
                       created: DateTime = DateTime.now())

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

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def fromUserId = column[Option[Long]]("from_user_id")
  def toUserId = column[Option[Long]]("to_user_id")
  def status = column[TransactionStatus]("status")
  def transactionType = column[TransactionType]("transaction_type")
  def amount = column[Long]("amount")
  def transactionId = column[Option[String]]("transaction_id", O.Unique)
  def created = column[DateTime]("created")

  def * = (
    id.?,
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
  import TransactionsTable._
  import profile.api._

  val logger = Logger(getClass)

  val Transactions = TableQuery[TransactionsTable]

  def sumUserBalance(user: User)(implicit ec: ExecutionContext): Future[Long] =
    db.run(Transactions
      .withFilter(tx => tx.fromUserId === user.id || tx.toUserId === user.id)
      .withFilter(tx => tx.status.inSet(Seq(TransactionStatus.Confirmed, TransactionStatus.Pending)))
      .map { tx =>
        Case
          // Credit, only include confirmed transactions
          .If(tx.toUserId === user.id && tx.status === TransactionStatus.Confirmed)
          .Then(tx.amount)
          // Debit, also include pending transactions
          .If(tx.fromUserId === user.id && (tx.status === TransactionStatus.Confirmed || tx.status === TransactionStatus.Pending))
          .Then(tx.amount * -1L)
          .Else(0L)
      }
      .sum
      .getOrElse(0L)
      .result)

  def insert(transaction: Transaction)(implicit ec: ExecutionContext): Future[Transaction] =
    db.run((Transactions returning Transactions.map(_.id)) += transaction)
      .map(id => transaction.copy(id = Some(id)))

  def update(transaction: Transaction)(implicit ec: ExecutionContext): Future[Transaction] =
    db.run(Transactions
      .withFilter(_.id === transaction.id)
      .update(transaction.copy(id = transaction.id))
      .map(_ => transaction))
}
