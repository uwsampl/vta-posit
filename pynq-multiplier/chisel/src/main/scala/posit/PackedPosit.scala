package vta.posit

import chisel3._
import chisel3.util._

class PackedPosit(implicit config: PositMultiplyConfig) extends Bundle {
  val bits = UInt(config.width.W)

  override def cloneType =
    new PackedPosit().asInstanceOf[this.type]
}

object PackedPositFunctions {
  def zeroPackedBits(implicit config: PositMultiplyConfig): UInt =
    0.U(config.width.W)

  def infPackedBits(implicit config: PositMultiplyConfig): UInt =
    Cat(1.U(1.W), 0.U((config.width - 1).W))
}
