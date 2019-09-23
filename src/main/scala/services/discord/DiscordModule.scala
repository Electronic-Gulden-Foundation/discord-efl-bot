package services.discord

import com.google.inject.AbstractModule
import net.dv8tion.jda.api.{JDA, JDABuilder}
import play.api.{Configuration, Environment}

class DiscordModule(environment: Environment,
                    configuration: Configuration)
  extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[DiscordMessageListener]).asEagerSingleton()
    bind(classOf[JDA]).toInstance(discordClient())
  }

  def discordClient(): JDA = {
    val token = configuration.get[String]("discord.token")

    new JDABuilder()
      .setToken(token)
      .build()
  }
}

