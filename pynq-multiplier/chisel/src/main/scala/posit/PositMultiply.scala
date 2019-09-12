// See README.md for license details.

package vta.posit

import chisel3._
import chisel3.util._

class PositMultiply(implicit config: PositMultiplyConfig) extends Module {
  val io = IO(new Bundle {
    val a = Input(new UnpackedPosit())
    val b = Input(new UnpackedPosit())
    val out = Output(new UnpackedPosit())
    val trailingBits = Output(UInt(config.trailingBits.W))
    val stickyBit = Output(UInt(1.W))
  })

  /* posit sign / fraction encoding (2s complement)-- Currently this is handled outside of PositMultiply before decoding inputs
  Posits always have a leading 1 */
  val abUnshiftedProduct = Cat(1.U(1.W), io.a.fraction) * Cat(1.U(1.W), io.b.fraction)

  /* The product result may be of the form 01.bbbb, or 1b.bbbb. In the latter
  case, our exponent is adjusted by 1. */
  val abExpShift = abUnshiftedProduct(config.fracProdBits - 1)

  /* FIXME: case where we are right at the limit, and the +1 from abExpShift
  causes an overflow? This might not be possible though except for some
  very specific (N, es) choices. */
  val exponentSum = io.a.exponent +& io.b.exponent
  val abExp = RegNext(exponentSum + abExpShift)
  val abSign = RegNext(io.a.sign ^ io.b.sign)
  val abShiftedProductCurr = Mux(abExpShift,
                                 abUnshiftedProduct,
                                 Cat(abUnshiftedProduct(config.fracProdBits - 2, 0), false.B))
  val abShiftedProduct = RegNext(abShiftedProductCurr)

  val zeroPadRight = Module(
    new ZeroPadRight(inWidth = config.fractionBits, outWidth = config.trailingBits))
  zeroPadRight.io.in := abShiftedProduct(config.fractionBits - 1, 0)
  val normalTrailingBits = zeroPadRight.io.out

  /* (a_unsigned - bias) + (b_unsigned - bias) >= min signed (-bias)
  a_u + b_u >= bias*/
  val abExpTooSmall = (abExp < config.exponent.bias.U)

  // Highest representable exponent is 2 * bias + MAX_UNSIGNED_EXPONENT
  val abExpTooLarge = (abExp > (config.exponent.bias + config.exponent.maxUnsigned).U)

  io.out.isInf := RegNext(io.a.isInf | io.b.isInf) // stray pipelining Assignment
  io.out.isZero := (~io.out.isInf) & (io.a.isZero | io.b.isZero)
  io.out.sign := (~io.out.isInf) & abSign

  val finalExpExtended = abExp - config.exponent.bias.U
  val finalExp = finalExpExtended(config.exponent.bitsUnsigned - 1, 0)

  val normalStickyBit =
    if (config.fractionBits - config.trailingBits >= 1) {
      abShiftedProduct(config.fractionBits - config.trailingBits - 1, 0).orR
    } else {
      false.B
    }

  val outSpecial = io.out.isZero | io.out.isInf | abExpTooSmall // added abExpTooSmall for non-zero underflow
  io.out.exponent := Mux(outSpecial,
                         0.U,
                         Mux(abExpTooLarge, config.exponent.maxUnsigned.U, finalExp))
  io.out.fraction := Mux(outSpecial || abExpTooLarge,
                         0.U,
                         abShiftedProduct(config.fracProdBits - 2, config.fractionBits + 1))

  val selZero = io.out.isInf | io.a.isZero | io.b.isZero | abExpTooLarge | abExpTooSmall
  io.trailingBits := Mux(selZero, 0.U, normalTrailingBits)
  io.stickyBit := Mux(selZero, 0.U, normalStickyBit)

  // prints to help while debugging PositMultiply
  if (config.debug.multiply || config.debug.all) {
    printf("\n")
    printf("Inside PositMultiply: \n")
    printf("abExpTooLarge is %b \n", abExpTooLarge)
    printf("abExpTooSmall is %b \n", abExpTooSmall)
    printf("a.exponent is %b \n", io.a.exponent)
    printf("b.exponent is %b \n", io.b.exponent)
    printf("io.out.exponent is %b \n", io.out.exponent)
    printf("finalEXp is %b \n", finalExp)
    printf("maxUnsignedExponent is %b \n", config.exponent.maxUnsigned.U)
    printf("abExp is %b\n", abExp)
    printf("abExpShift is %b \n", abExpShift)
    printf("\n")
  }
}
