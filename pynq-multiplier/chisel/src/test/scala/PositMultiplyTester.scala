// See README.md for license details.
package unittest

import chisel3._
import chisel3.core.Bundle
import chisel3.util._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import vta.posit._
import unittest.util._

class PositMultiplyTester(c: PositMultiplyPackedToPacked) extends PeekPokeTester(c) {

  val posits = new DecimalToPosit
  val random = scala.util.Random
  val positArray = posits.arrayOfPosits
  val round = new RoundDouble
  //Currently assumes inputs are always two's comp based, and only works now if twosComp = true (standard)
  implicit val config = PositMultiplyConfig(8, 1, 2)
  var res = posits.singlePosit(0.000244140625).U(config.width.W)
  var a = posits.singlePosit(0.000244140625).U(config.width.W) //1.U(config.width.W)
  var b = posits.singlePosit(0.000244140625).U(config.width.W) //1.U(config.width.W)
  //Testing underflow (exponent too small) with smallest positive Posit (must round down to zero)
  poke(c.io.a.bits, a)
  poke(c.io.b.bits, b)
  step(2)
  expect(c.io.out.bits, if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "smallest positive Posit times itself rounds to 0")
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")
  

  //Testing underflow (exponent too small) with smallest negative Posit (must round down to zero)
  a = posits.singlePosit(-0.000244140625).U
  b = posits.singlePosit(-0.000244140625).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "smallest negative Posit times itself rounds to 0 "
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  //Testing underflow (exponent too small) with smallest negative & positive Posit (must round down to zero)
  a = posits.singlePosit(0.000244140625).U
  b = posits.singlePosit(-0.000244140625).U
  res = posits.singlePosit(-0.000244140625).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(c.io.out.bits, if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "smallest negative Posit times smallest positive rounds to 0 ")
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  //Testing overflow with largest positive Posit
  a = posits.singlePosit(4096.0).U
  b = posits.singlePosit(4096.0).U
  res = posits.singlePosit(4096.0).U(config.width.W)
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "largest positive Posit times itself rounds to 4096 "
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  //Testing overflow with largest negative Posit
  a = posits.singlePosit(-4096.0).U
  b = posits.singlePosit(-4096.0).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "largest negative Posit times itself rounds to 4096 "
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  //Testing overflow with largest positive Posit times largest negative
  a = posits.singlePosit(4096.0).U
  b = posits.singlePosit(-4096.0).U
  res = posits.singlePosit(-4096.0).U(config.width.W)
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "largest positive Posit times largest positive rounds to 4096 "
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")
  
  //Testing positive * positive Posit, no Rounding: 8 * 12 = 96
  a = posits.singlePosit(8.0).U
  b = posits.singlePosit(12.0).U
  res = posits.singlePosit(96.0).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "8 * 12 = 96 (pos. * pos., without rounding)"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")
  
  //Testing positive * positive Posit, with Rounding: 4.5 * 5 = 22.5 ~ 24
  a = posits.singlePosit(4.5).U
  b = posits.singlePosit(5.0).U
  res = posits.singlePosit(24.0).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "4.5 * 5 = ~24 (pos. * pos., with rounding)"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")
  
  //Testing positive * negative Posit, with Rounding: 4.5 * -5 = -22.5 ~ -24
  a = posits.singlePosit(4.5).U
  b = posits.singlePosit(-5.0).U
  res = posits.singlePosit(-24.0).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "4.5 * -5 = ~-24 (pos. * neg., with rounding)"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  //Testing positive * negative Posit, without Rounding: 4 * -5 = -20
  a = posits.singlePosit(4.0).U
  b = posits.singlePosit(-5.0).U
  res = posits.singlePosit(-20.0).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    "4 * -5 = -20 (pos. * neg., without rounding)"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  //Testing negative * negative Posit, without Rounding: -6 * -8 = 48
  a = posits.singlePosit(-6.0).U
  b = posits.singlePosit(-8.0).U
  res = posits.singlePosit(48.0).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    " -6 * -8 = 48 (neg. * neg.,without rounding)"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  //Testing negative * negative Posit, with Rounding: -7 * -6.5 = 48
  a = posits.singlePosit(-7.0).U
  b = posits.singlePosit(-6.5).U
  res = posits.singlePosit(48.0).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    " -6.5 * -7 = 48 (neg. * neg.,with rounding)"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  a = posits.singlePosit(4.0).U
  b = posits.singlePosit(3.375).U
  res = posits.singlePosit(14).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    " 4.0 * 3.375 = 14 (rounding up from midpoint)"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")
 
  a = posits.singlePosit(4.5).U
  b = posits.singlePosit(2.875).U
  res = posits.singlePosit(13.0).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    " 4.5 * 2.875 = 13 (rounding slightly up))"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")
 
  a = posits.singlePosit(4.0).U
  b = posits.singlePosit(3.125).U
  res = posits.singlePosit(12.0).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    " 4.5 * 3.125 = 12 (rounding down from midpoint)"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  a = posits.singlePosit(4.0).U
  b = posits.singlePosit(Double.NaN).U
  res = posits.singlePosit(Double.NaN).U
  poke(c.io.a.bits, if (config.twosComp || a(config.width - 1) == false) { a } else {
    Cat(a(config.width - 1), (~a(config.width - 2, 0) + 1.U))
  })
  poke(c.io.b.bits, if (config.twosComp || b(config.width - 1) == false) { b } else {
    Cat(b(config.width - 1), (~b(config.width - 2, 0) + 1.U))
  })
  step(2)
  expect(
    c.io.out.bits,
    if (config.twosComp || res(config.width - 1) == false) { res } else {
      Cat(res(config.width - 1), (~res(config.width - 2, 0) + 1.U))
    },
    " 4.5 * 3.125 = 12 (rounding down from midpoint)"
  )
  println(s"${posits.decimalOf(a.toInt)} (b'${a.toInt}) * ${posits.decimalOf(b.toInt)} (b'${b.toInt}) = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (b'${peek(c.io.out.bits).toInt}) ")

  var expectedResult: Int = 0
  var exactResult: Double = 0.0
  var roundedResult: Double = 0.0
  var result: Double = 0.0
  var gotExpectedResult: Boolean = false
  for (i <- 0 until 15) {
    a = (random.nextInt(256) + 0).U
    b = (random.nextInt(256) + 0).U
    poke(c.io.a.bits, a)
    poke(c.io.b.bits, b)
    exactResult = posits.decimalOf(a.toInt) * posits.decimalOf(b.toInt)
    roundedResult = round.roundToNearestEven(exactResult)
    result = posits.decimalOf(peek(c.io.out.bits).toInt)
    step(2)
    expectedResult = posits.singlePosit(roundedResult)
    gotExpectedResult = (peek(c.io.out.bits).toInt == expectedResult) || (peek(c.io.out.bits).toInt == expectedResult - 1) || (peek(c.io.out.bits).toInt == expectedResult + 1)
    println(s"${posits.decimalOf(a.toInt)} * ${posits.decimalOf(b.toInt)} = ${posits.decimalOf(peek(c.io.out.bits).toInt)} (exact, non-posit result is $exactResult)")
    // Expect is not used right now because the rounding can't be perfectly simulated, so only guaranteed to expect something within +/- one of the correct answer
    // expect(c.io.out.bits, expectedResult)
    println(s"The output is within 1 posit of what the poorly rounded simulation expects: $gotExpectedResult") // this is true if the multiplier's output is within +/- one of the simulated expected result
    println("\n")
  }

}
