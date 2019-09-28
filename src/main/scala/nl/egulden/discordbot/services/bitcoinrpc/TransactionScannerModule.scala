package nl.egulden.discordbot.services.bitcoinrpc

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment, Mode}

class TransactionScannerModule(environment: Environment,
                               configuration: Configuration)
  extends AbstractModule {

  override def configure(): Unit = {
    environment.mode match {
      case Mode.Dev | Mode.Prod =>
        bind(classOf[TransactionScanner]).asEagerSingleton()

      case _ =>
        // Do nothing
    }

  }

}
