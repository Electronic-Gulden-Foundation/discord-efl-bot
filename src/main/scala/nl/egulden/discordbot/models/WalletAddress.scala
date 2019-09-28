package nl.egulden.discordbot.models

import com.github.tototoshi.slick.MySQLJodaSupport._
import javax.inject.Inject
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object WalletAddressConfig {
  val LOCK_DAYS = 14
}

case class WalletAddress(id: Option[Long] = None,
                         currentUserId: Long,
                         address: String,
                         lockedUntil: DateTime = DateTime.now.plusDays(WalletAddressConfig.LOCK_DAYS),
                         created: DateTime = DateTime.now)

class WalletAddressesTable(tag: Tag) extends Table[WalletAddress](tag, "wallet_addresses") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def currentUserId = column[Long]("current_user_id")
  def address = column[String]("address")
  def lockedUntil = column[DateTime]("locked_until")
  def created = column[DateTime]("created")

  def * = (
    id.?,
    currentUserId,
    address,
    lockedUntil,
    created
  ) <> (WalletAddress.tupled, WalletAddress.unapply)
}

class WalletAddressesDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val WalletAddresses = TableQuery[WalletAddressesTable]

  def byAddress(address: String): Future[Option[WalletAddress]] =
    db.run(WalletAddresses
      .withFilter(_.address === address)
      .result
      .headOption)

  def deleteAll(): Future[Int] =
    db.run(WalletAddresses.delete)

  def getExistingForUser(userId: Long): Future[Option[WalletAddress]] =
    db.run(WalletAddresses
      .withFilter(_.currentUserId === userId)
      .result
      .headOption)

  def getUnusedAddress(): Future[Option[WalletAddress]] =
    db.run(WalletAddresses
      .withFilter(_.lockedUntil < DateTime.now)
      .result
      .headOption)

  def insert(address: WalletAddress)(implicit ec: ExecutionContext): Future[WalletAddress] =
    db.run((WalletAddresses returning WalletAddresses.map(_.id)) += address)
      .map(id => address.copy(id = Some(id)))

  def update(address: WalletAddress)(implicit ec: ExecutionContext): Future[WalletAddress] =
    db.run(WalletAddresses
      .withFilter(_.id === address.id)
      .update(address.copy(id = address.id))
      .map(_ => address))
}
