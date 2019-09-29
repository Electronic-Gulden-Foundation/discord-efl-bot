package nl.egulden.discordbot.services

import javax.inject.Inject
import nl.egulden.discordbot.models.{SettingEntry, SettingsDAO}

import scala.concurrent.{ExecutionContext, Future}

class SettingsService @Inject()(settingsDAO: SettingsDAO) {

  def get[T <: Serializable](key: String)
            (implicit ec: ExecutionContext): Future[Option[T]] =
    settingsDAO.findByKey(key).map {
      case Some(settingEntry) => settingEntry.value.map(_.asInstanceOf[T])
      case None => None
    }

  def set[T <: Serializable](key: String, value: T)
                            (implicit ec: ExecutionContext): Future[SettingEntry] =
    set(key, Some(value))

  def set[T <: Serializable](key: String, value: Option[T])
                            (implicit ec: ExecutionContext): Future[SettingEntry] =
    settingsDAO.insertOrUpdate(SettingEntry(key, value.map(_.toString)))

}
