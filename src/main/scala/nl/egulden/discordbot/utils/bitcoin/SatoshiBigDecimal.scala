package nl.egulden.discordbot.utils.bitcoin

object SatoshiBigDecimal {
  implicit def bigDecimalToSatoshi(bd: BigDecimal): SatoshiBigDecimal = new SatoshiBigDecimal(bd)
  implicit def doubleToSatoshi(d: Double): SatoshiBigDecimal = new SatoshiBigDecimal(d)
  implicit def intToSatoshi(i: Int): SatoshiBigDecimal = new SatoshiBigDecimal(i)
}

class SatoshiBigDecimal(val value: BigDecimal) {
  val Satoshi = 1e-8

  def satoshi: BigDecimal = satoshis
  def satoshis: BigDecimal = value * Satoshi

  def toSatoshi: Long = toSatoshis
  def toSatoshis: Long = (value / Satoshi).toLong
}
