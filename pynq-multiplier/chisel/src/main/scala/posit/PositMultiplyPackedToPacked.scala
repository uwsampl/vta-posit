package vta.posit

import chisel3._
import chisel3.util._
import scala.math._

class PositMultiplyPackedToPacked(implicit config: PositMultiplyConfig) extends Module {
  val io = IO(new Bundle {
    val a = Input(new PackedPosit())
    val b = Input(new PackedPosit())
    val out = Output(new PackedPosit())
    val trailingBits = Output(UInt(config.trailingBits.W))
    val stickyBit = Output(UInt(1.W))
  })

  val decodeA = Module(new PositDecode())
  val packedA = if (config.twosComp) {
    val invertedA = Cat(io.a.bits(config.width - 1), (~io.a.bits(config.width - 2, 0) + 1.U))
      .asTypeOf(new PackedPosit())
    Mux(io.a.bits(config.width - 1), invertedA, io.a)
  } else {
    io.a
  }
  decodeA.io.in := packedA
  val unpackedA = RegNext(decodeA.io.out)

  val decodeB = Module(new PositDecode())
  val packedB = if (config.twosComp) {
    val invertedB = Cat(io.b.bits(config.width - 1), (~io.b.bits(config.width - 2, 0) + 1.U))
      .asTypeOf(new PackedPosit())
    Mux(io.b.bits(config.width - 1), invertedB, io.b)
  } else {
    io.b
  }
  decodeB.io.in := packedB
  val unpackedB = RegNext(decodeB.io.out)

  val multiply = Module(new PositMultiply())
  multiply.io.a := unpackedA
  multiply.io.b := unpackedB
  val unpackedOut = multiply.io.out
  io.trailingBits := multiply.io.trailingBits
  io.stickyBit := multiply.io.stickyBit

  // print statements for debugging/checking PositRoundToNearestEven
  if (config.debug.roundToNearestEvenExternal || config.debug.all) {
    printf("before rounding: \n")
    printf("out.fraction is %b \n", unpackedOut.fraction)
    printf("out.exponent is %b \n", unpackedOut.exponent)
    printf("unsignedRegime is %b \n", UnpackedPositFunctions.unsignedRegime(unpackedOut))
    printf("signed Regime is %b \n", UnpackedPositFunctions.signedRegime(unpackedOut))
  }

  val round = Module(new PositRoundToNearestEven())
  round.io.in := unpackedOut
  round.io.trailingBits := io.trailingBits
  round.io.stickyBit := io.stickyBit
  val roundOut = round.io.out

  // print statements for debugging/checking PositRoundToNearestEven
  if (config.debug.roundToNearestEvenExternal || config.debug.all) {
    printf("after rounding: \n")
    printf("out.fraction is %b \n", roundOut.fraction)
    printf("out.exponent is %b \n", roundOut.exponent)
    printf("unsignedRegime is %b \n", UnpackedPositFunctions.unsignedRegime(roundOut))
  }

  val encode = Module(new PositEncode())
  encode.io.in := roundOut
  val packedOut = if (config.twosComp) {
    val invertedOut =
      Cat(encode.io.out.bits(config.width - 1), (~encode.io.out.bits(config.width - 2, 0) + 1.U))
        .asTypeOf(new PackedPosit())
    Mux(encode.io.out.bits(config.width - 1), invertedOut, encode.io.out)
  } else {
    encode.io.out
  }
  io.out := packedOut
}

object Elaborate extends App {
  implicit val config = PositMultiplyConfig(8, 1, 2)
  chisel3.Driver.execute(args, () => new PositMultiplyPackedToPacked)

}
