package nl.egulden.discordbot.services.bitcoinrpc

import javax.inject.{Inject, Named}
import nl.egulden.discordbot.models.{User, WalletAddress, WalletAddressConfig, WalletAddressesDAO}
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

class WalletAddressService @Inject()(walletAddressesDAO: WalletAddressesDAO,
                                     @Named("egulden") eguldenRpcClient: BitcoinRpcClient) {

  def getOrCreateAddressFor(user: User)
                           (implicit ec: ExecutionContext): Future[WalletAddress] =
    for {
      maybeExisting <- walletAddressesDAO.getExistingForUser(user.id.get)

      newOrExisting <- maybeExisting match {
        case Some(address) => Future(address)
        case None => findUnusedOrCreateNewAddress(user)
      }

      updatedWithUsage <- updateAddressUsage(newOrExisting, user)
    } yield updatedWithUsage

  def findAddress(address: String): Future[Option[WalletAddress]] =
    walletAddressesDAO.byAddress(address)

  def updateAddressUsage(address: WalletAddress, user: User)
                        (implicit ec: ExecutionContext): Future[WalletAddress] =
    updateAddressUsage(address, user.id.get)

  def updateAddressUsage(address: WalletAddress, userId: Long)
                        (implicit ec: ExecutionContext): Future[WalletAddress] =
    walletAddressesDAO.update(address.copy(
      currentUserId = userId,
      lockedUntil = DateTime.now.plusDays(WalletAddressConfig.LOCK_DAYS)
    ))

  private def findUnusedOrCreateNewAddress(user: User)
                                          (implicit ec: ExecutionContext): Future[WalletAddress] =
    walletAddressesDAO.getUnusedAddress()
      .flatMap {
        case Some(address) => Future(address)
        case None => createNewWalletAddress(user)
      }

  private def createNewWalletAddress(user: User)
                                    (implicit ec: ExecutionContext): Future[WalletAddress] =
    for {
      addressStr <- eguldenRpcClient.getNewAddress()
      walletAddress <- walletAddressesDAO.insert(WalletAddress(
        currentUserId = user.id.get,
        address = addressStr,
        lockedUntil = DateTime.now.plusDays(WalletAddressConfig.LOCK_DAYS)
      ))
    } yield walletAddress
}
