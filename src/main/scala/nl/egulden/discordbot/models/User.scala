package nl.egulden.discordbot.models

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class User(id: Option[Long] = None,
                discordUserId: Long,
                created: LocalDateTime = LocalDateTime.now())

class UsersTable(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
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

  def insert(user: User)(implicit ec: ExecutionContext): Future[User] =
    db.run((Users returning Users.map(_.id)) += user)
      .map(id => user.copy(id = id))

  def update(user: User)(implicit ec: ExecutionContext): Future[User] =
    db.run(Users
      .withFilter(_.id === user.id)
      .update(user.copy(id = user.id)))
      .map(_ => user)
}
