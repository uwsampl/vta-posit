// See README.md for license details.

package vta.posit

import chisel3._
import chisel3.util._
import scala.math._

case class RegimeConfig(width: Int = 8, es: Int = 1) {
  val maxFieldSize = width - 1
  val maxSigned = maxFieldSize - 1
  val maxUnsigned = maxSigned * 2
  val minSigned = -maxSigned
  val minUnsigned = 0
  val bitsSigned = log2Ceil(maxSigned + 1) + 1
  val bitsUnsigned = bitsSigned
}

case class ExponentConfig(width: Int = 8, es: Int = 1) {
  val regime = RegimeConfig(width, es)
  val bitsUnsigned = regime.bitsUnsigned + es
  val maxUnsigned = pow(2, es).toInt * regime.maxUnsigned
  val bias = pow(2, es).toInt * regime.maxSigned
  val biasBits = log2Ceil(bias)
  val prodBits = bitsUnsigned + 1
}

case class PositMultiplyDebugConfig() {
  val all = false
  val multiply = false
  val roundToNearestEvenExternal = false // for print statements in PositMultiplyPackedToPacked
  val roundToNearestEvenInternal = false // for inside PositRoundToNearestEven
  val shiftRightSticky = false
  val countLeadingZeros = false
  val decode = false
  val roundHelper = false
}

case class PositMultiplyConfig(width: Int = 8, es: Int = 1, trailingBits: Int = 2) {
  val regime = RegimeConfig(width, es)
  val exponent = ExponentConfig(width, es)
  val debug = PositMultiplyDebugConfig()
  val esBits = if (es > 1) {
    es
  } else {
    1
  }

  val fractionBits = if (width - 1 - 2 - es <= 0) {
    1
  } else {
    width - 1 - 2 - es
  }

  val fracProdBits = (fractionBits + 1) * 2
  val shiftRoundSize = 1 + es + fractionBits
  val twosComp = true
}
