package vta.posit

import chisel3._
import chisel3.util._

class PositDecode(implicit config: PositMultiplyConfig) extends Module {
  val io = IO(new Bundle {
    val in = Input(new PackedPosit())
    val out = Output(new UnpackedPosit())
  })

  // FIXME: I've changed the posit layout to not be symmetric, to have simpler decoding
  val remainderBits = io.in.bits(config.width - 2, 0) // Bits after the sign, with the sign adjusted via 2s complement

  /* For determining the regime, we use a leading zero counter on the xor of
  neighboring bits in the input, to determine where the 0 -> 1 or 1 -> 0
  transition occurs, and thus the regime */
  val remainderXorVec =
    VecInit.tabulate(config.regime.maxFieldSize - 1)(i => remainderBits(i + 1) ^ remainderBits(i))

  /* chisel won't allow assigning to partial bits of a value, so
  assign remainderXor to remainderXorVec and use remainderXor everywhere else */
  val remainderXor = remainderXorVec.asUInt

  val countingLeadingZeros = Module(new CountLeadingZeros(width = config.regime.maxFieldSize - 1))
  countingLeadingZeros.io.in := remainderXor

  // The count of leading zeros of remaindorXor; this is how far we need to shift our word for the ES and fraction bits
  val leadingZeroCount = countingLeadingZeros.io.out
  leadingZeroCount.suggestName("leadingZeroCount")

  /* Whether the regime is positive or negative depends upon the first non-sign
  bit in the input */
  val regimePosOrZero = remainderBits(config.regime.maxFieldSize - 1)

  // are we +/- infinity or zero
  val isSpecial = ~remainderBits.orR

  // Added this because extending in one line where also negating or adding did not work
  val leadingZeroCountExtended = Wire(UInt(config.regime.bitsUnsigned.W))
  leadingZeroCountExtended := leadingZeroCount
  leadingZeroCount.suggestName("leadingZeroCountExtended")
  // Calculated regime value starting from 0
  val unsignedRegime = Mux(
    isSpecial,
    0.U(config.regime.bitsUnsigned.W),
    Mux(
      regimePosOrZero,
      leadingZeroCount.asTypeOf(UInt(config.regime.bitsUnsigned.W)) + config.regime.maxSigned.U,
      config.regime.maxSigned.U + ~leadingZeroCount.asTypeOf(UInt(config.regime.bitsUnsigned.W))
    )
  )
  unsignedRegime.suggestName("unsignedRegime")
  /* Can we just do this?
     signedRegime = regPosOrZero ? leadingZeroCount : ~leadingZeroCount
     unsignedRegime = signedRegime + config.regime.maxSigned

     The number of bits to encode the regime is really
     min(max(leadingZeroCount, cl1) + 1, config.regime.maxFieldSize):

     Regime containing all 0s is either 0 or +/- inf
     For WIDTH = 5, config.regime.maxFieldSize = 4:

      0 or +/- inf
                |    min representable exponent
                v    v
     encoding 0000 0001 001x 01xx 10xx 110x 1110 1111
     sgn regime  x   -3   -2   -1    0    1    2    3
     uns regime  x    0    1    2    3    4    5    6
     cl0(xor)    3    2    1    0    0    1    2    3  cl0 is leadingZeroCount
     regime bits 4    4    3    2    2    3    4    4

     However, we use the count of the regime bits to shift our
     word into place to extract the ES and fraction bits.
     Note that at the extreme positive and negative regime, we are
     consuming all bits in the word, so we needn't take into
     account the outer min.
     The leading zero counter effectively produces the regime shift - 2.
     The regime at least takes up two bits, so this is perfect.
   */

  val topEsAndFractionBit: Int = config.regime.maxFieldSize - 3

  /* remainderBits, skipping first two bits (min size of a regime excoding),
  shifted by extra regime bits */
  val esAndFractionBits = remainderBits(topEsAndFractionBit, 0) << leadingZeroCount // should be  Wire(UInt((config.regime.maxFieldSize - 2).W))

  val outFraction = if (config.es > 0) { // We have a ES to extract
    esAndFractionBits(topEsAndFractionBit - config.es, 0)
  } else { // There is no ES to extract
    esAndFractionBits(topEsAndFractionBit, 0)
  }

  val outExponent = if (config.es > 0) { // We have a ES to extract
    /* The entire ES field may not be present (it could be truncated),
    but the shift above will ensure that we are only reading 0s for the
    other values*/
    val esBits = esAndFractionBits(topEsAndFractionBit, topEsAndFractionBit - config.es + 1)
    Cat(unsignedRegime, esBits)
  } else { // There is no ES to extract
    unsignedRegime
  }

  io.out.fraction := outFraction
  io.out.exponent := outExponent

  io.out.isInf := io.in.bits(config.width - 1) & isSpecial
  io.out.isZero := ~io.in.bits(config.width - 1) & isSpecial
  io.out.sign := ~isSpecial & io.in.bits(config.width - 1)

  // print statements to use while debugging PositDecode
  if (config.debug.decode || config.debug.all) {
    printf("unsignedRegime is %b \n", unsignedRegime)
    printf("remainderXor is %b \n", remainderXor)
    printf("esAndFractionBits is %b \n", esAndFractionBits)
    printf("remainderBits is %b \n", remainderBits)
    printf("leadingZeroCount (regime shift - 2) is %b \n", leadingZeroCount)
    printf("~leadingZeroCountExtended is %b \n", ~leadingZeroCountExtended)
    printf("config.regime.maxS is %b \n", config.regime.maxSigned.U)
    printf("regimePosOrZero is %b \n", regimePosOrZero)
    printf("config.regime.maxFieldSize is %b \n", config.regime.maxFieldSize.U)
  }

}
