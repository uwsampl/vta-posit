/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
// taken from https://github.com/freechipsproject/chisel-testers
package test.example

import chisel3._
import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.TestRunner
import unittest._
import vta.posit._
import example._



object Launcher {
  //implicit val p = new Parameters(size = 8)
  implicit val config = new PositMultiplyConfig(8, 1, 2)
  val tests = Map(  
    "positMultiply" -> { (manager: TesterOptionsManager) =>
      Driver.execute(() => new PositMultiplyPackedToPacked, manager) {
        (c) => new PositMultiplyTester(c)
      }
    }

  )

  def main(args: Array[String]): Unit = {
    TestRunner(tests, args)
  }
}
