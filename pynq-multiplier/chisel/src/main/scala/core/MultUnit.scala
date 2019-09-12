package vta.core
import vta.posit._
import chisel3._
import chisel3.util._
import vta.util.config._
import vta.shell._
//import example._
class Mult(aBits: Int = 8, bBits: Int = 8) extends Module {
  val outBits = aBits + bBits
  val numUnits = aBits/8
  val io = IO(new Bundle {
    val a = Flipped(ValidIO(UInt(aBits.W)))
    val b = Flipped(ValidIO(UInt(bBits.W)))
    val y = Output(UInt(outBits.W))
  })

  val rA = RegEnable(io.a.bits.asUInt, io.a.valid)
  val rB = RegEnable(io.b.bits.asUInt, io.b.valid)
  
  implicit val config = PositMultiplyConfig(8, 1, 2)
 
  val aInputs = rA.asTypeOf(Vec(numUnits, UInt(8.W)))
  val bInputs = rB.asTypeOf(Vec(numUnits, UInt(8.W)))

  val stickyBit = Wire(Vec(numUnits, UInt(1.W)))
  val trailingBits = Wire(Vec(numUnits, UInt(config.trailingBits.W)))
  val multOutputs = Wire(Vec(numUnits, UInt(config.width.W)))
  val vecMultiply = for(i <- 0 until numUnits) yield {
    val positMult = Module(new PositMultiplyPackedToPacked)
    positMult.io.a.bits := aInputs(i)
    positMult.io.b.bits := bInputs(i)
    stickyBit(i) := positMult.io.stickyBit
    trailingBits(i) := positMult.io.trailingBits
    multOutputs(i) := positMult.io.out.bits
  }
  
  io.y := multOutputs.asTypeOf(io.y)
}

class MultUnit(debug: Boolean = true)(implicit p: Parameters) extends Module {
  val vp = p(ShellKey).vcrParams
  val mp = p(ShellKey).memParams
  val io = IO(new Bundle {
    val start = Input(Bool())
    val done = Output(Bool())
    val ecnt = Vec(vp.nECnt, ValidIO(UInt(vp.regBits.W)))
    val vals = Input(Vec(vp.nVals, UInt(vp.regBits.W)))
    val ptrs = Input(Vec(vp.nPtrs, UInt(mp.addrBits.W)))
    val vme_rd = new VMEReadMaster
    val vme_wr = new VMEWriteMaster
  })
  val cnt = RegInit(0.U(8.W))
  val op_num = 2
  val op_cnt = RegInit(0.U(log2Ceil(op_num).W))
  val len = io.vals(0)
  val raddr = RegInit(VecInit(Seq.fill(op_num)(0.U(mp.addrBits.W))))
  val waddr = RegInit(0.U(mp.addrBits.W))

  val sIdle :: sReadCmd :: sReadData :: sWriteCmd :: sWriteData :: sWriteAck :: Nil = Enum(6)
  val state = RegInit(sIdle)

  // control
  switch (state) {
    is (sIdle) {
      when (io.start) {
        state := sReadCmd
      }
    }
    is (sReadCmd) {
      when (io.vme_rd.cmd.ready) {
        state := sReadData
      }
    }
    is (sReadData) {
      when (io.vme_rd.data.valid) {
        when (op_cnt === (op_num - 1).U) {
          state := sWriteCmd
        } .otherwise {
          state := sReadCmd
        }
      }
    }
    is (sWriteCmd) {
      when (io.vme_wr.cmd.ready) {
        state := sWriteData
      }
    }
    is (sWriteData) {
      when (io.vme_wr.data.ready) {
        state := sWriteAck
      }
    }
    is (sWriteAck) {
      when (io.vme_wr.ack) {
        when(cnt === len) {
          state := sIdle
        } .otherwise {
          state := sReadCmd
        }
      }
    }
  }

  when (state === sWriteCmd) {
    op_cnt := 0.U
  } .elsewhen (io.vme_rd.data.fire()) {
    op_cnt := op_cnt + 1.U
  }

  val mult = Module(new Mult(aBits = 64, bBits = 64))

  mult.io.a.valid := op_cnt === 0.U & io.vme_rd.data.fire()
  mult.io.b.valid := op_cnt === 1.U & io.vme_rd.data.fire()
  mult.io.a.bits := io.vme_rd.data.bits(63, 0)
  mult.io.b.bits := io.vme_rd.data.bits(63, 0)

  when (state === sIdle) {
    cnt := 0.U
  } .elsewhen (io.vme_wr.data.fire()) {
    cnt := cnt + 1.U
  }

  val last = state === sWriteAck & io.vme_wr.ack & cnt === len
  val cycles = RegInit(0.U(vp.regBits.W))

  // cycle counter
  when (state === sIdle) {
    cycles := 0.U
  } .otherwise {
    cycles := cycles + 1.U
  }

  io.ecnt(0).valid := last
  io.ecnt(0).bits := cycles

  when (state === sIdle) {
    raddr(0) := io.ptrs(0)
    raddr(1) := io.ptrs(1)
    waddr := io.ptrs(2)
  } .elsewhen (state === sWriteAck && io.vme_wr.ack) { // increment by 8-bytes
    raddr(0) := raddr(0) + 8.U
    raddr(1) := raddr(1) + 8.U
    waddr := waddr + 8.U
  }

  io.vme_rd.cmd.valid := state === sReadCmd
  io.vme_rd.cmd.bits.addr := Mux(op_cnt === 0.U, raddr(0), raddr(1))
  io.vme_rd.cmd.bits.len := 0.U

  io.vme_rd.data.ready := state === sReadData

  io.vme_wr.cmd.valid := state === sWriteCmd
  io.vme_wr.cmd.bits.addr := waddr
  io.vme_wr.cmd.bits.len := 0.U

  io.vme_wr.data.valid := state === sWriteData
  io.vme_wr.data.bits := mult.io.y

  io.done := last

  // debug
  if (debug) {
    when (io.vme_wr.cmd.fire()) {
      printf("[MultUnit] [AW] addr:%x len:%x\n", io.vme_wr.cmd.bits.addr, io.vme_wr.cmd.bits.len)
    }
    when (io.vme_wr.data.fire()) {
      printf("[MultUnit] [W] data:%x\n", io.vme_wr.data.bits)
    }
    when (io.vme_rd.cmd.fire()) {
      printf("[MultUnit] [AR] addr:%x len:%x\n", io.vme_rd.cmd.bits.addr, io.vme_rd.cmd.bits.len)
    }
    when (io.vme_rd.data.fire()) {
      printf("[MultUnit] [R] data:%x\n", io.vme_rd.data.bits)
    }
  }
}
