package vta.posit

import chisel3._
import chisel3.util._

/** Round helper for posits that performs the proper fraction/ES truncation based
  * on exponent, so we can determine the bits that need rounding (for arithmetic
  * or geometric rounding)
  */
class PositRoundHelper(implicit config: PositMultiplyConfig) extends Module {
  val io = IO(new Bundle {
    val in = Input(new UnpackedPosit())
    val inTrailingBits = Input(UInt(config.trailingBits.W))
    val inStickyBit = Input(UInt(1.W))

    /* The adjusted (ES, fraction) with possible space for ES overflow
    FIXME: remove ES overflow bit
    This is (0, ES, fraction) right shifted by excessRegimeBits */
    val postShift = Output(UInt((config.shiftRoundSize + config.trailingBits).W))

    /* We assume the regime always takes two bits in the encoding. This determines
    how many more bits we need to shift. This is also the number of bits that
    postShift has been shifted right */
    val excessRegimeBits = Output(UInt((config.regime.bitsUnsigned - 1).W))

    // The resulting sticky bit from the shift
    val outStickyBit = Output(UInt(1.W))
  })

  val preShift = if (config.es == 0) {
    Cat(0.U(1.W), Cat(io.in.fraction, io.inTrailingBits))
  } else {
    Cat(0.U(1.W), Cat(io.in.exponent(config.es - 1, 0), Cat(io.in.fraction, io.inTrailingBits)))
  }

  /** General algorithm:
  We have a fixed bit width exponent / fraction which may or may not overflow
  in a posit representation.

  If the exponent is within bounds, it is still possible that some (or all)
  of the fraction bits will be truncated, or some (or all) of the ES bits
  will be truncated.

  Based on the exponent, we determine whether or not overflow will occur, and
  we determine how many of the [ES, fraction] bits will go away.
  No truncation happens if the regime takes 2 bits, which is the minimum.
  Any additional regime bit results in truncation.
  excessRegimeBits is the number of bits that we need to truncate and shift
  by. */
  val shiftRightSticky = Module(
    new ShiftRightSticky(inWidth = config.shiftRoundSize + config.trailingBits,
                         outWidth = config.shiftRoundSize + config.trailingBits,
                         shiftValWidth = config.regime.bitsUnsigned - 1))

  shiftRightSticky.io.in := preShift
  shiftRightSticky.io.shift := io.excessRegimeBits
  val postShiftSticky = shiftRightSticky.io.sticky
  val unusedStickyAnd = shiftRightSticky.io.stickyAnd

  assert(config.exponent.bitsUnsigned - config.es == config.regime.bitsUnsigned)

  val unsignedRegime =
    UnpackedPositFunctions.unsignedRegime(io.in)

  val signedRegime = UnpackedPositFunctions.signedRegime(io.in)

  /* The FRACTION_BITS is based on the maximum possible fraction size, that is
  N - 3. Any regime encoding with more than 2 bits will truncate the
  fraction and/or ES.

  Example:
     encoding 0000 0001 001x 01xx 10xx 110x 1110 1111
     sgn regime  x   -3   -2   -1    0    1    2    3
     uns regime  x    0    1    2    3    4    5    6
     regime bits 4    4    3    2    2    3    4    4
     exc bits    x    2    1    0    0    1    2    3(*)
  FIXME: (*) not true for max regime. Does this matter? */
  io.excessRegimeBits := Mux(signedRegime >= 0.S,
                             signedRegime(config.regime.bitsSigned - 2, 0).asUInt,
                             (~signedRegime(config.regime.bitsSigned - 2, 0)).asUInt)
  io.outStickyBit := io.inStickyBit | postShiftSticky
  io.postShift := shiftRightSticky.io.out

  // print statements for debugging PositRoundHelper
  if (config.debug.roundHelper || config.debug.all) {
    printf("config.regime.bitsU - 1 is %d\n", config.regime.bitsUnsigned.U - 1.U)
    printf("in roundHelper, preShift is %b\n", preShift)
  }
}
