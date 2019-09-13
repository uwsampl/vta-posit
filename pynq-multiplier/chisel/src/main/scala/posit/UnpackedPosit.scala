package vta.posit

import chisel3._

class UnpackedPosit(implicit config: PositMultiplyConfig) extends Bundle {
  val isZero = Bool()
  val isInf = Bool()
  val sign = Bool()
  val exponent = UInt(config.exponent.bitsUnsigned.W) // 5.W for 8,1
  val fraction = UInt(config.fractionBits.W) // 4.W for 8,1

  override def cloneType =
    new UnpackedPosit().asInstanceOf[this.type]

}

object UnpackedPositFunctions {
  def signedRegime(unpackedPosit: UnpackedPosit)(implicit config: PositMultiplyConfig): SInt = {
    val answer = Wire(SInt(config.regime.bitsSigned.W))
    answer := (unpackedPosit.exponent(config.exponent.bitsUnsigned - 1, config.es) -
      config.regime.maxSigned.asUInt).asSInt()
    return answer
  }

  def unsignedRegime(unpackedPosit: UnpackedPosit)(implicit config: PositMultiplyConfig): UInt = {
    val answer = unpackedPosit.exponent(config.exponent.bitsUnsigned - 1, config.es)
    return answer
  }
}
