package vta.posit

import chisel3._
import chisel3.util._

class PositEncode(implicit config: PositMultiplyConfig) extends Module {
  val io = IO(new Bundle {
    val in = Input(new UnpackedPosit())
    val out = Output(new PackedPosit())
  })

  val signedRegime = UnpackedPositFunctions.signedRegime(io.in)
  val posRegime = (signedRegime >= 0.S)

  val firstBits = Mux(posRegime === 1.U, 2.U(2.W), 1.U(2.W))

  val esAndFraction = if (config.es > 0) {
    Cat(firstBits, Cat(io.in.exponent(config.es - 1, 0), io.in.fraction)).asSInt()
  } else {
    Cat(firstBits, io.in.fraction).asSInt()
  }

  /* Example:
    encoding 0000 0001 001x 01xx 10xx 110x 1110 1111
    sgn regime  x   -3   -2   -1    0    1    2    3
    uns regime  x    0    1    2    3    4    5    6
    regime bits 4    4    3    2    2    3    4    4

    Equivalent of posRegime ? signedRegime : -signedRegime - 1
    Our shift width only needs to encode the maximum positive regime */
  val shiftBits = Mux(posRegime === 1.U,
                      signedRegime(config.regime.bitsSigned - 2, 0).asUInt(),
                      (~signedRegime(config.regime.bitsSigned - 2, 0)).asUInt())

  // arithmetic right shift, to extend the leading 1 if present
  val esAndFractionShifted = esAndFraction >> shiftBits
  // above seems to work, getting rid of: val esAndFractionShifted = Wire(SInt(config.regime.maxFieldSize.W))

  // select between special cases or the regular posit encoding
  val zero = PackedPositFunctions.zeroPackedBits
  val inf = PackedPositFunctions.infPackedBits
  val outputOtherwise = Cat(io.in.sign, esAndFractionShifted(config.width - 2, 0))

  io.out.bits := Mux(io.in.isZero, zero, Mux(io.in.isInf, inf, outputOtherwise))

}
