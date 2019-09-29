package nl.egulden.discordbot.services.bitcoinrpc

import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Provider}
import play.api.{Configuration, Environment, Logger, Mode}

class BitcoinRpcModule(environment: Environment,
                       configuration: Configuration)
  extends AbstractModule {

  val logger = Logger(getClass)
  val root = "bitcoinrpc"

  override def configure(): Unit = {
    environment.mode match {
      case Mode.Dev | Mode.Prod =>
        configuration.get[Configuration](root).keys
          .filter(_.contains("."))
          .map(_.split("\\.")(0))
          .foreach(bindRpcClient)

      case _ =>
        // Do nothing
    }
  }

  def bindRpcClient(name: String): ScopedBindingBuilder = {
    logger.info(s"Binding bitcoin rpc for config $name")

    val host: String = configuration.get[String](s"$root.$name.host")
    val port: Int = configuration.get[Int](s"$root.$name.port")
    val username: String = configuration.get[String](s"$root.$name.username")
    val password: String = configuration.get[String](s"$root.$name.password")

    bind(classOf[BitcoinRpcClient])
      .annotatedWith(Names.named(name))
      .toProvider(new Provider[BitcoinRpcClient] {
        override def get(): BitcoinRpcClient =
          new BitcoinRpcClient(host, port, username, password)
      })
  }
}
