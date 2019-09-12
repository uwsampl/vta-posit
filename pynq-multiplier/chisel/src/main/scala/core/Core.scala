package vta.core

import chisel3._
import chisel3.util._
import vta.util.config._
import vta.shell._

case class CoreParams (
  batch: Int = 1,
  blockOut: Int = 16,
  blockIn: Int = 16,
  inpBits: Int = 8,
  wgtBits: Int = 8,
  uopBits: Int = 32,
  accBits: Int = 32,
  outBits: Int = 8,
  uopMemDepth: Int = 512,
  inpMemDepth: Int = 512,
  wgtMemDepth: Int = 512,
  accMemDepth: Int = 512,
  outMemDepth: Int = 512,
  instQueueEntries: Int = 32
)

case object CoreKey extends Field[CoreParams]

class Core(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val vcr = new VCRClient
    val vme = new VMEMaster
  })
  val rp = 0
  val s1_launch = RegNext(io.vcr.launch)
  val start = io.vcr.launch & ~s1_launch
  val mult = Module(new MultUnit)

  mult.io.start := start
  io.vcr.ecnt <> mult.io.ecnt
  mult.io.vals <> io.vcr.vals
  mult.io.ptrs <> io.vcr.ptrs
  mult.io.vme_rd <> io.vme.rd(rp)
  mult.io.vme_wr <> io.vme.wr(0)

  // tie-off other read-ports
  for (i <- 0 until 5) {
    if (i != rp) {
      io.vme.rd(i).cmd.valid := false.B
      io.vme.rd(i).cmd.bits.addr := 0.U
      io.vme.rd(i).cmd.bits.len := 0.U
      io.vme.rd(i).data.ready := false.B
    }
  }

  // finish
  val finish = RegNext(mult.io.done)
  io.vcr.finish := finish
}
