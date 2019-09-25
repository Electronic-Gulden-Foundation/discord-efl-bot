package nl.egulden.discordbot.services.discord

import java.util
import java.util.concurrent.{ExecutorService, ScheduledExecutorService}

import com.google.inject.AbstractModule
import com.google.inject.util.Providers
import net.dv8tion.jda.api.entities.{ApplicationInfo, Category, Emote, Guild, PrivateChannel, Role, SelfUser, StoreChannel, TextChannel, User, VoiceChannel, Webhook}
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.managers.{AudioManager, DirectAudioController, Presence}
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.GuildAction
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.cache.{CacheView, SnowflakeCacheView}
import net.dv8tion.jda.api.{AccountType, JDA, JDABuilder, Permission}
import okhttp3.OkHttpClient
import play.api.{Configuration, Environment, Mode}

class DiscordModule(environment: Environment,
                    configuration: Configuration)
  extends AbstractModule {

  override def configure(): Unit = {
    environment.mode match {
      case mode if (mode == Mode.Dev || mode == Mode.Prod) =>
        bind(classOf[DiscordMessageListener]).asEagerSingleton()
        bind(classOf[JDA]).toInstance(discordClient())

      case _ =>
        bind(classOf[JDA]).toInstance(testingDiscordClient())
    }
  }

  private def discordClient(): JDA = {
    val token = configuration.get[String]("discord.token")

    new JDABuilder()
      .setToken(token)
      .build()
  }

  private def testingDiscordClient(): JDA = {
    new JDA {
      override def addEventListener(listeners: Any*): Unit = ???
      override def getInviteUrl(permissions: Permission*): String = ???
      override def getMutualGuilds(users: User*): util.List[Guild] = ???
      override def removeEventListener(listeners: Any*): Unit = ???

      override def awaitStatus(status: JDA.Status): JDA = ???
      override def createGuild(name: String): GuildAction = ???
      override def getAccountType: AccountType = ???
      override def getAudioManagerCache: CacheView[AudioManager] = ???
      override def getCallbackPool: ExecutorService = ???
      override def getCategoryCache: SnowflakeCacheView[Category] = ???
      override def getDirectAudioController: DirectAudioController = ???
      override def getEmoteCache: SnowflakeCacheView[Emote] = ???
      override def getEventManager: IEventManager = ???
      override def getGatewayPing: Long = ???
      override def getGatewayPool: ScheduledExecutorService = ???
      override def getGuildCache: SnowflakeCacheView[Guild] = ???
      override def getHttpClient: OkHttpClient = ???
      override def getInviteUrl(permissions: util.Collection[Permission]): String = ???
      override def getMaxReconnectDelay: Int = ???
      override def getMutualGuilds(users: util.Collection[User]): util.List[Guild] = ???
      override def getPresence: Presence = ???
      override def getPrivateChannelCache: SnowflakeCacheView[PrivateChannel] = ???
      override def getRateLimitPool: ScheduledExecutorService = ???
      override def getRegisteredListeners: util.List[Any] = ???
      override def getResponseTotal: Long = ???
      override def getRoleCache: SnowflakeCacheView[Role] = ???
      override def getSelfUser: SelfUser = ???
      override def getShardInfo: JDA.ShardInfo = ???
      override def getShardManager: ShardManager = ???
      override def getStatus: JDA.Status = ???
      override def getStoreChannelCache: SnowflakeCacheView[StoreChannel] = ???
      override def getTextChannelCache: SnowflakeCacheView[TextChannel] = ???
      override def getToken: String = ???
      override def getUserCache: SnowflakeCacheView[User] = ???
      override def getVoiceChannelCache: SnowflakeCacheView[VoiceChannel] = ???
      override def isAutoReconnect: Boolean = ???
      override def isBulkDeleteSplittingEnabled: Boolean = ???
      override def retrieveApplicationInfo(): RestAction[ApplicationInfo] = ???
      override def retrieveUserById(id: Long): RestAction[User] = ???
      override def retrieveUserById(id: String): RestAction[User] = ???
      override def retrieveWebhookById(webhookId: String): RestAction[Webhook] = ???
      override def setAutoReconnect(reconnect: Boolean): Unit = ???
      override def setEventManager(manager: IEventManager): Unit = ???
      override def setRequestTimeoutRetry(retryOnTimeout: Boolean): Unit = ???
      override def shutdown(): Unit = ???
      override def shutdownNow(): Unit = ???
    }
  }
}

