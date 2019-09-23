package models

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class User(id: Option[Long],
                discordUserId: Long,
                created: LocalDateTime)

class UsersTable(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Option[Long]]("id", O.PrimaryKey)
  def discordUserId = column[Long]("discord_user_id", O.Unique)
  def created = column[LocalDateTime]("created")

  def * = (
    id,
    discordUserId,
    created
  ) <> (User.tupled, User.unapply)
}

@Singleton
class UsersDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  private val Users = TableQuery[UsersTable]

  def byDiscordUserId(discordUserId: Long): Future[Option[User]] =
    db.run(Users.withFilter(_.discordUserId === discordUserId).result.headOption)

}
