// See README.md for license details.

package vta.posit

import chisel3._
import chisel3.util._

/** In the case that zero is passed, the keep bit is by definition zero, but the
  * trailing and sticky bits may be non-zero, in which case we may round up to
  * the minimum posit.
  */
class PositRoundToNearestEven(implicit config: PositMultiplyConfig) extends Module {
  val io = IO(new Bundle {
    val in = Input(new UnpackedPosit())
    val trailingBits = Input(UInt(2.W))
    val stickyBit = Input(UInt(1.W))
    val out = Output(new UnpackedPosit())

  })

  val roundHelper = Module(new PositRoundHelper())
  roundHelper.io.in := io.in
  roundHelper.io.inTrailingBits := io.trailingBits
  roundHelper.io.inStickyBit := io.stickyBit
  val postShift = roundHelper.io.postShift
  val excessRegimeBits = roundHelper.io.excessRegimeBits
  val roundStickyBit = roundHelper.io.outStickyBit

  val r2ne = Module(new RoundToNearestEven)
  r2ne.io.keepBit := postShift(2)
  r2ne.io.trailingBits := postShift(1, 0)
  r2ne.io.stickyBit := roundStickyBit
  val roundDown = r2ne.io.roundDown // Round Logic

  val postShiftRound = postShift(config.shiftRoundSize + config.trailingBits - 1,
                                 config.trailingBits) + ~roundDown

  // The (es, fraction) realigned
  val reShift = postShiftRound << excessRegimeBits // inferring Wire(UInt(config.shiftRoundSize.W))

  // Increment the regime if there was a carry in the round increment above
  val roundUnsignedRegime = UnpackedPositFunctions.unsignedRegime(io.in) +
    reShift(config.shiftRoundSize - 1)

  val postRoundExponent = if (config.es == 0) {
    roundUnsignedRegime
  } else {
    Cat(roundUnsignedRegime,
        reShift(config.shiftRoundSize - 2,
                config.shiftRoundSize -
                  2 - config.es + 1))
  }

  val overflow = (roundUnsignedRegime >= config.regime.maxUnsigned.U)

  /* If we have a zero, we may still round up in these cases, as the keep
  bit is by definition 0:
    x | 1 0 1 : round up
    x | 1 1 0 : round up
    x | 1 1 1 : round up
   */
  val zeroRoundUp = io.in.isZero & (io.trailingBits(1) & (io.trailingBits(0) | io.stickyBit)) // We use the original input trailing and sticky bits

  io.out.sign := io.in.sign
  io.out.isZero := io.in.isZero & ~zeroRoundUp
  io.out.isInf := io.in.isInf

  val inSpecial = io.in.isZero | io.in.isInf
  io.out.exponent := Mux(inSpecial,
                         0.U,
                         Mux(overflow, config.exponent.maxUnsigned.U, postRoundExponent))
  io.out.fraction := Mux(inSpecial || overflow, 0.U, reShift(config.fractionBits - 1, 0))

  // print statements for debugging PositRoundToNearestEven
  if (config.debug.roundToNearestEvenInternal || config.debug.all) {
    printf("rounded fraction is %b \n", io.out.fraction)
    printf("overflow is %b \n", overflow)
    printf("reshift is %b \n", reShift(config.fractionBits - 1, 0))
    printf("roundUnsignedRegime is %b \n", roundUnsignedRegime)
    printf("excessRegimeBits is %b \n", excessRegimeBits)
    printf("postShiftRound is %b\n", postShiftRound)
    printf("postShift is %b \n", postShift)
    printf("roundDown is %b \n", roundDown)
  }
}
