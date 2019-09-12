// See README.md for license details.

package vta.posit

import chisel3._
import chisel3.util._

class RoundToNearestEven extends Module {
  val io = IO(new Bundle {
    val keepBit = Input(UInt(1.W))
    val trailingBits = Input(UInt(2.W))
    val stickyBit = Input(UInt(1.W))
    val roundDown = Output(UInt(1.W))
  })

  /* Round to nearest even behavior:
    K | G R S
    x | 0 x x : round down (truncate)
    0 | 1 0 0 : round down (truncate)
    1 | 1 0 0 : round up
    x | 1 0 1 : round up
    x | 1 1 0 : round up
    x | 1 1 1 : round up
   */
  io.roundDown := ~io.trailingBits(1) |
    (~io.keepBit & io.trailingBits(1) & ~io.trailingBits(0) & ~io.stickyBit)

}
