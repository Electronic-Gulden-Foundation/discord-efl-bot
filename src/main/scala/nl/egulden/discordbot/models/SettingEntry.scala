package nl.egulden.discordbot.models

import com.github.tototoshi.slick.MySQLJodaSupport._
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class SettingEntry(key: String,
                        value: Option[String])

class SettingsTable(tag: Tag) extends Table[SettingEntry](tag, "settings") {
  def key = column[String]("key", O.PrimaryKey)
  def value = column[Option[String]]("value")

  def * = (
    key,
    value
  ) <> (SettingEntry.tupled, SettingEntry.unapply)
}

class SettingsDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  val Settings = TableQuery[SettingsTable]

  def findByKey(key: String): Future[Option[SettingEntry]] =
    db.run(Settings.withFilter(_.key === key).result.headOption)

  def insertOrUpdate(setting: SettingEntry)
                    (implicit ec: ExecutionContext): Future[SettingEntry] =
    db.run(Settings.insertOrUpdate(setting))
      .map(_ => setting)
}
