package nl.egulden.discordbot.services.tipping

import javax.inject.Inject
import nl.egulden.discordbot.models.{Transaction, User}
import nl.egulden.discordbot.services.models.UsersService
import nl.egulden.discordbot.services.tipping.TippingError.TippingError

import scala.concurrent.{ExecutionContext, Future}

object TippingError extends Enumeration {
  type TippingError = Value

  val NotEnoughBalance = Value
}

class TippingService @Inject()(usersService: UsersService,
                               tipWalletService: TipWalletService) {

  def tip(tippingUser: User, tippedUser: User, amount: Double)
         (implicit ec: ExecutionContext): Future[Either[Transaction, TippingError]] =
    tipWalletService.hasBalance(tippingUser, amount).flatMap {
      case true =>
        tipWalletService.createTipTransaction(tippingUser, tippedUser, amount)
          .map(Left(_))

      case false =>
        Future(Right(TippingError.NotEnoughBalance))
    }
}
