package nl.egulden.discordbot.services.bitcoinrpc

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient._

import nl.egulden.discordbot.utils.bitcoin.SatoshiBigDecimal._

import scala.concurrent.{ExecutionContext, Future, blocking}

class BitcoinRpcClient(host: String,
                       port: Int,
                       user: String,
                       password: String) {

  val client = new BitcoinJSONRPCClient(s"http://$user:$password@$host:$port")

  def getMiningInfo()(implicit ec: ExecutionContext): Future[MiningInfo] = Future {
    blocking {
      client.getMiningInfo
    }
  }

  def getBalance()(implicit ec: ExecutionContext): Future[Long] = Future {
    blocking {
      BigDecimal(client.getBalance.toString).toSatoshis
    }
  }

  def getNewAddress()(implicit ec: ExecutionContext): Future[String] = Future {
    blocking {
      client.getNewAddress
    }
  }

  def getRawTransaction(txId: String)
                       (implicit ec: ExecutionContext): Future[RawTransaction] = Future {
    blocking {
      client.getRawTransaction(txId)
    }
  }


  def listSinceBlock(maybeBlockhash: Option[String])
                    (implicit ec: ExecutionContext): Future[TransactionsSinceBlock] = Future {
    blocking {
      maybeBlockhash match {
        case Some(blockhash) => client.listSinceBlock(blockhash)
        case None => client.listSinceBlock()
      }
    }
  }

  def sendToAddress(address: String,
                    amount: Long,
                    maybeComment: Option[String] = None)
                   (implicit ec: ExecutionContext): Future[String] = Future {
    blocking {
      maybeComment match {
        case Some(comment) =>
          client.sendToAddress(address, amount.satoshis.bigDecimal, comment)

        case None =>
          client.sendToAddress(address, amount.satoshis.bigDecimal)
      }
    }
  }
}
