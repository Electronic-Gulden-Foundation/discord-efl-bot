package nl.egulden.discordbot.services.bitcoinrpc

import java.time.Duration

import akka.util.Timeout
import nl.egulden.discordbot.models.{User, WalletAddress, WalletAddressConfig, WalletAddressesDAO}
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.await

import scala.concurrent.{ExecutionContext, Future}

class WalletAddressServiceTest extends PlaySpec with MockFactory {

  trait TestSetup {
    implicit val timeout = Timeout.create(Duration.ofSeconds(5))
    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

    lazy val user = User(id = Some(1L), discordUserId = 10L)
    lazy val defaultAddress = WalletAddress(
      currentUserId = user.id.get,
      address = "someaddress"
    )

    lazy val walletAddressesDAO = stub[WalletAddressesDAO]
    (walletAddressesDAO.insert(_: WalletAddress)(_: ExecutionContext)).when(*, *).onCall { (walletAddress, ec) => Future(walletAddress)(ec) }
    (walletAddressesDAO.update(_: WalletAddress)(_: ExecutionContext)).when(*, *).onCall { (walletAddress, ec) => Future(walletAddress)(ec) }

    lazy val bitcoinRpcClient = stub[BitcoinRpcClient]

    lazy val walletAddressService = new WalletAddressService(
      walletAddressesDAO = walletAddressesDAO,
      bitcoinRpcClient
    )
  }

  "WalletAddressService" should {
    "create a new rpc address if required" in new TestSetup {
      (walletAddressesDAO.getExistingForUser _).when(*).returning(Future(None)(ec)).once
      (walletAddressesDAO.getUnusedAddress _).when().returning(Future(None)(ec)).once

      (bitcoinRpcClient.getNewAddress()(_: ExecutionContext)).when(*).returning(Future("newrpcaddress")).once

      val address = await(walletAddressService.getOrCreateAddressFor(user))

      address.address mustBe "newrpcaddress"
      address.currentUserId mustBe user.id.get
      address.lockedUntil.isAfter(DateTime.now.plusDays(WalletAddressConfig.LOCK_DAYS - 1))
    }

    "use an existing address if available" in new TestSetup {
      (walletAddressesDAO.getExistingForUser _).when(*).returning(Future(None)).once
      (walletAddressesDAO.getUnusedAddress _).when().returning(Future(Some(defaultAddress.copy(address = "someexistingaddress")))) .once
      (bitcoinRpcClient.getNewAddress()(_: ExecutionContext)).when(*).never

      val address = await(walletAddressService.getOrCreateAddressFor(user))

      address.address mustBe "someexistingaddress"
      address.currentUserId mustBe user.id.get
      address.lockedUntil.isAfter(DateTime.now.plusDays(WalletAddressConfig.LOCK_DAYS - 1))
    }

    "use the users 'own' address if available" in new TestSetup {
      (walletAddressesDAO.getExistingForUser _).when(*).returning(Future(Some(defaultAddress.copy(address = "someexistingaddress")))).once
      (walletAddressesDAO.getUnusedAddress _).when().never
      (bitcoinRpcClient.getNewAddress()(_: ExecutionContext)).when(*).never

      val address = await(walletAddressService.getOrCreateAddressFor(user))

      address.address mustBe "someexistingaddress"
      address.currentUserId mustBe user.id.get
      address.lockedUntil.isAfter(DateTime.now.plusDays(WalletAddressConfig.LOCK_DAYS - 1))
    }
  }
}
