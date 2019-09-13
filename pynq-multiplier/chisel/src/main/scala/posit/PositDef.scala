package vta.posit

import chisel3._
import chisel3.util._
import scala.math._

/** Pads an input with zeros on the right, handles cases where no padding is
  * required too (thus, this can't be done with the Verilog concatenation
  * operator as it would result in zero-sized fields)
  */
class ZeroPadRight(inWidth: Int = 8, outWidth: Int = 8) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(inWidth.W))
    val out = Output(UInt(outWidth.W))
  })

  if (outWidth < inWidth) {
    io.out := io.in(inWidth - 1, inWidth - 1 - outWidth)
  } else if (outWidth == inWidth) {
    io.out := io.in
  } else {
    io.out := Cat(io.in, Fill((outWidth - inWidth), false.B))
  }

}

class ShiftRightSticky(outWidth: Int = 8, inWidth: Int = 8, shiftValWidth: Int = log2Ceil(8 + 1))(
    implicit config: PositMultiplyConfig)
    extends Module {

  val io = IO(new Bundle {
    val in = Input(UInt(inWidth.W))
    val shift = Input(UInt(shiftValWidth.W))
    val out = Output(UInt(outWidth.W))
    val sticky = Output(UInt(1.W))
    val stickyAnd = Output(UInt(1.W))
  })
  val shiftMax: Int = pow(2, shiftValWidth).toInt - 1

  val numSteps: Int = if (shiftMax >= outWidth) {
    log2Ceil(outWidth)
  } else {
    log2Ceil(shiftMax)
  }

  val valVector = Wire(Vec(numSteps + 1, UInt(outWidth.W)))
  val valVectorOfVecs = Wire(Vec(numSteps + 1, Vec(outWidth, Bool())))

  valVector := valVectorOfVecs.asTypeOf(valVector)

  val valStickyVec = Wire(Vec(numSteps + 1, Bool()))
  val valSticky = valStickyVec.asUInt

  val valStickyAndVec = Wire(Vec(numSteps + 1, Bool()))
  val valStickyAnd = valStickyAndVec.asUInt

  val maxShift = Wire(UInt(1.W))

  val padding = Module(new ZeroPadRight(inWidth, outWidth))
  padding.io.in := io.in
  // valVector(0) is set to padding.io.out
  for (i <- 0 to outWidth - 1) {
    valVectorOfVecs(0)(i) := padding.io.out(i)
  }

  if (inWidth <= outWidth) {
    valStickyVec(0) := 0.U
    valStickyAndVec(0) := 1.U
  } else {
    valStickyVec(0) := io.in(inWidth - outWidth - 1, 0).orR
    valStickyAndVec(0) := io.in(inWidth - outWidth - 1, 0).andR
  }

  for (i <- 1 to numSteps) {
    for (j <- 0 until outWidth) {
      if ((j + pow(2, i - 1)) >= outWidth) {
        valVectorOfVecs(i)(j) := Mux(io.shift(i - 1) === true.B, false.B, valVectorOfVecs(i - 1)(j))
      } else {
        valVectorOfVecs(i)(j) := Mux(io.shift(i - 1) === true.B,
                                     valVectorOfVecs(i - 1)(j + pow(2, i - 1).toInt),
                                     valVectorOfVecs(i - 1)(j))
      }
    }
    val stickyHelper =
      Mux(io.shift(i - 1) === true.B, valVector(i - 1)(pow(2, i - 1).toInt - 1, 0).orR, false.B)
    val stickyAndHelper =
      Mux(io.shift(i - 1) === true.B, valVector(i - 1)(pow(2, i - 1).toInt - 1, 0).andR, true.B)
    valStickyVec(i) := valStickyVec(i - 1) | stickyHelper
    valStickyAndVec(i) := valStickyAndVec(i - 1) & stickyAndHelper

  }
  val result = Wire(UInt(outWidth.W))
  val resultSticky = Wire(UInt(1.W))
  val resultStickyAnd = Wire(UInt(1.W))

  if (shiftMax < outWidth) {
    maxShift := 0.U
    result := valVector(numSteps)
    resultSticky := valSticky(numSteps)
    resultStickyAnd := valStickyAnd(numSteps)

  } else {
    if (shiftMax == outWidth) {
      maxShift := (io.shift === outWidth.U)
    } else {
      maxShift := (io.shift >= outWidth.U)
    }
    result := Mux(maxShift === true.B, 0.U(outWidth.U), valVector(numSteps))
    resultSticky := Mux(maxShift === true.B, valVector(0).orR | valSticky(0), valSticky(numSteps))
    resultStickyAnd := Mux(maxShift === true.B,
                           valVector(0).andR & valStickyAnd(0),
                           valStickyAnd(numSteps))
  }

  io.out := result
  io.sticky := resultSticky
  io.stickyAnd := resultStickyAnd

  // print statements to debug ShiftRightSticky
  if (config.debug.shiftRightSticky || config.debug.all) {
    printf("shift is %b\n", io.shift)
    printf("valVector(numSteps) is %b\n", valVector(numSteps))
    printf("out is %b\n", io.out)
    printf("in is %b\n", io.in)
    printf("numSteps is %d\n", numSteps.U)
    printf("shiftValWidth is %d\n outWidth is %d\n inWidth is %d\n shiftMax is %d\n",
           shiftValWidth.U,
           outWidth.U,
           inWidth.U,
           shiftMax.U)
  }
}

class CountLeadingZerosTree(leftTreeWidth: Int = 8, rightTreeWidth: Int = 8)(
    implicit config: PositMultiplyConfig)
    extends Module {
  val io = IO(new Bundle {
    val left = Input(UInt(leftTreeWidth.W))
    val right = Input(UInt(rightTreeWidth.W))
    val out = Output(UInt(log2Ceil(leftTreeWidth + rightTreeWidth + 1).W))
  })

  def largestPowerOf2Divisor(x: Int): Int = pow(2, log2Ceil(x) - 1).toInt

  // leftTreeWidth is always a power of 2; rightTreeWidth might not be
  val leftTreeWidth2: Int = leftTreeWidth / 2

  // The new leftTreeWidth for the right-hand recursion should be a power of 2 as well
  val rightTreeWidth2A: Int = largestPowerOf2Divisor(rightTreeWidth)

  // floor(rightTreeWidth / 2)
  val rightTreeWidth2B: Int = rightTreeWidth - rightTreeWidth2A

  assert(leftTreeWidth > 0, s"the leftTreeWidth must be greater than 0")
  assert(rightTreeWidth > 0, s"the rightTreeWidth must be greater than 0")
  assert(log2Ceil(leftTreeWidth) == log2Ceil(leftTreeWidth + 1) - 1,
         s"log2Ceil(leftTreeWidth) must equal log2Ceil(leftTreeWidth + 1) - 1")
  assert(leftTreeWidth >= rightTreeWidth,
         s"leftTreeWidth must be greater or equal to the rightTreeWidth")

  val lCount = Wire(UInt(log2Ceil(leftTreeWidth + 1).W))
  val rCount = Wire(UInt(log2Ceil(rightTreeWidth + 1).W))

  val rCountExtend =
    if ((log2Ceil(leftTreeWidth + 1) - 1 - (log2Ceil(rightTreeWidth + 1) - 1)) == 0) {
      rCount
    } else {
      Cat(Fill(log2Ceil(leftTreeWidth + 1) - 1 - (log2Ceil(rightTreeWidth + 1) - 1), false.B),
          rCount)
    }

  if (leftTreeWidth >= 2) {
    val leftCount = Module(
      new CountLeadingZerosTree(leftTreeWidth = leftTreeWidth2, rightTreeWidth = leftTreeWidth2))
    leftCount.io.left := io.left(leftTreeWidth - 1, leftTreeWidth - 1 - leftTreeWidth2 + 1)
    leftCount.io.right := io.left(leftTreeWidth2 - 1, 0)
    lCount := leftCount.io.out
  } else {
    lCount := ~io.left(0)
  }

  if (rightTreeWidth >= 2) {
    val rightCount = Module(
      new CountLeadingZerosTree(leftTreeWidth = rightTreeWidth2A,
                                rightTreeWidth = rightTreeWidth2B))
    rightCount.io.left := io.right(rightTreeWidth - 1, rightTreeWidth - 1 - rightTreeWidth2A + 1)
    rightCount.io.right := io.right(rightTreeWidth2B - 1, 0)
    rCount := rightCount.io.out
  } else {
    rCount := ~io.right(0)
  }
  val result = Wire(UInt(log2Ceil(leftTreeWidth + rightTreeWidth + 1).W))

  if (log2Ceil(leftTreeWidth + 1) > 1) {
    result := Mux(
      lCount(log2Ceil(leftTreeWidth + 1) - 1) && rCountExtend(log2Ceil(leftTreeWidth + 1) - 1),
      Cat(1.U(1.W), 0.U((log2Ceil(leftTreeWidth + rightTreeWidth + 1) - 1).W)),
      Mux(!lCount(log2Ceil(leftTreeWidth + 1) - 1),
          Cat(0.U(1.W), lCount),
          Cat(1.U(2.W), rCountExtend(log2Ceil(leftTreeWidth + 1) - 2, 0)))
    )
  } else {
    result := Mux(
      lCount(log2Ceil(leftTreeWidth + 1) - 1) && rCountExtend(log2Ceil(leftTreeWidth + 1) - 1),
      Cat(1.U(1.W), 0.U((log2Ceil(leftTreeWidth + rightTreeWidth + 1) - 1).W)),
      Mux(!lCount(log2Ceil(leftTreeWidth + 1) - 1), Cat(0.U(1.W), lCount), 1.U(2.W))
    )
  }

  io.out := result

  if (config.debug.countLeadingZeros || config.debug.all) {
    printf(
      "%d %d: left %b right %b lcount %b rcount %b rcountext %b out %b \n",
      leftTreeWidth.asUInt,
      rightTreeWidth.asUInt,
      io.left,
      io.right,
      lCount,
      rCount,
      rCountExtend,
      io.out
    )
  }

}

class CountLeadingZeros(width: Int = 6, addOffset: Int = 0)(implicit config: PositMultiplyConfig)
    extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(width.W))
    val out = Output(UInt(log2Ceil(width + 1 + addOffset).W))
  })

  def largestPowerOf2Divisor(x: Int): Int = pow(2, log2Ceil(x) - 1).toInt

  val leftTreeWidth: Int = largestPowerOf2Divisor(width + addOffset)
  val rightTreeWidth: Int = width + addOffset - leftTreeWidth

  assert(leftTreeWidth >= rightTreeWidth,
         s"leftTreeWidth must be greater or equal to the rightTreeWidth")
  assert(leftTreeWidth > 0, s"the leftTreeWidth must be greater than 0")
  assert(rightTreeWidth > 0, s"the rightTreeWidth must be greater than 0")

  val inPad = if (addOffset == 0) {
    io.in
  } else {
    Cat(Fill(addOffset, 0.U), io.in) //inPadVec.asUInt
  }

  val tree = Module(
    new CountLeadingZerosTree(leftTreeWidth = leftTreeWidth, rightTreeWidth = rightTreeWidth))
  tree.io.left := inPad(width + addOffset - 1, width + addOffset - 1 - leftTreeWidth + 1)
  tree.io.right := io.in(rightTreeWidth - 1, 0)
  io.out := tree.io.out

}
