package nl.egulden.discordbot.services.models

import javax.inject.Inject
import nl.egulden.discordbot.models.{User, UsersDAO}

import scala.concurrent.{ExecutionContext, Future}

class UsersService @Inject()(usersDAO: UsersDAO) {

  def findOrCreateUserByDiscordId(discordUserId: Long)
                                 (implicit ec: ExecutionContext): Future[User] =
    usersDAO.byDiscordUserId(discordUserId)
      .flatMap {
        case Some(user) => Future(user)
        case _ => usersDAO.insert(User(
          discordUserId = discordUserId
        ))
      }
}
