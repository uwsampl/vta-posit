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

/*!
 *  Copyright (c) 2018 by Contributors
 * \file metal_test.cpp
 * \brief Bare-metal test to test driver and VTA design.
 */

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <vta/driver.h>
#ifdef VTA_TARGET_PYNQ
#  include "../../../src/pynq/pynq_driver.h"
#endif  // VTA_TARGET_PYNQ
#include "../common/test_lib.h"

#define VTA_BASE_ADDR 0x43c00000
#define VTA_DATA_LEN 8
#define VTA_DATA_BYTES VTA_DATA_LEN*8

void fill_data(int64_t* a, int64_t* b, int64_t* c, uint32_t len) {
  for (uint32_t i = 0; i < len; i++) {
    c[i] = 0xdeadbeefdeadbeef;
    if (i == 0) {
      //  0.000244140625 (0x01) *  0.000244140625 (0x01)
      //  0.000244140625 (0x01) * -0.000244140625 (0xff)
      // -0.000244140625 (0xff) * -0.000244140625 (0xff)
      //  0.000244140625 (0x01) *  0.0 (0x00)
      a[i] = 0x01ffff01010101;
      b[i] = 0x00ffffffff0101;
    } else if (i == 1) {
      //  12 (0x6c) * NaN (0x80)
      //  12 (0x6c) * 4096 (0x7f) 
      //  4096 (0x7f) * -4096 (0x81)
      // -4096 (0x81) * -4096 (0x81)
      a[i] = 0x81817f7f6c6c6c6c; 
      b[i] = 0x818181817f7f8080; 
    } else if (i == 2) {
      // 8 (0x68) * 12 (0x6c)
      // 4.5 (0x61) *  5.0 (0x62)
      // 4.5 (0x61) * -5.0 (0x9e)
      // 4.0 (0x60) * -5.0 (0x9e)
      a[i] = 0x6060616161616868; 
      b[i] = 0x9e9e9e9e62626c6c; 
    } else if (i == 3) {
      // -6.0 (0x9c) * -8.0   (0x98)
      // -7.0 (0x9a) * -6.5   (0x9b)
      //  4.0 (0x60) *  3.375 (0x5b)
      //  4.5 (0x61) *  2.875 (0x57)
      a[i] = 0x616160609a9a9c9c;
      b[i] = 0x57575b5b9b9b9898;
    } else if (i == 4) {
      // 4.0 (0x60) * 3.125 (0x59)
      // 4.0 (0x60) * NaN (0x80)
      a[i] = 0x6060606060606060;
      b[i] = 0x8080808059595959;
    } else if (i == 5) {
      // -6 * -8
      a[i] = 156; 
      b[i] = 152;
    } else if (i == 6) {
      // -7 * -6.5
      a[i] = 154;
      b[i] = 155;
    } else if (i == 7) {
      // -4096 * 4096
      a[i] = 129;
      b[i] = 127;
    } else {
      a[i] = (rand() % 60) + 60;
      b[i] = (rand() % 60) + 60;
    }

  }
}

void print_data(int64_t* a, int64_t* b, int64_t* c, uint32_t len) {
  for (uint32_t i = 0; i < len; i++) {
    printf("[%u] a:%lld \t b:%lld \t c:%lld\n", i,(uint64_t)a[i], (uint64_t)b[i], c[i]);
  }
}

int main(void) {

  void* a;
  void* b;
  void* c;

  a = VTAMemAlloc(VTA_DATA_BYTES, 0);
  b = VTAMemAlloc(VTA_DATA_BYTES, 0);
  c = VTAMemAlloc(VTA_DATA_BYTES, 0);

  fill_data((int64_t*)a, (int64_t*)b, (int64_t*)c, VTA_DATA_LEN);

  void* vta_handle = VTAMapRegister(VTA_BASE_ADDR);

  VTAWriteMappedReg(vta_handle, 0x04, 0x0);
  VTAWriteMappedReg(vta_handle, 0x08, VTA_DATA_LEN);
  VTAWriteMappedReg(vta_handle, 0x0c, VTAMemGetPhyAddr(a));
  VTAWriteMappedReg(vta_handle, 0x10, VTAMemGetPhyAddr(b));
  VTAWriteMappedReg(vta_handle, 0x14, VTAMemGetPhyAddr(c));
  VTAWriteMappedReg(vta_handle, 0x00, 1);

  int flag = 0, t = 0, cycles = 0;
  for (t = 0; t < 10000000; ++t) {
    flag = VTAReadMappedReg(vta_handle, 0x0);
    if (flag & 2) break;
  }

  cycles = VTAReadMappedReg(vta_handle, 0x4);

  VTAUnmapRegister(vta_handle);

  printf("cycles:%d\n", cycles);
  print_data((int64_t*)a, (int64_t*)b, (int64_t*) c, VTA_DATA_LEN);

  VTAMemFree(a);
  VTAMemFree(b);
  VTAMemFree(c);

  return 0;
}
