package vta.core

import vta.util.config._

class CoreConfig extends Config((site, here, up) => {
  case CoreKey => CoreParams(
    batch = 1,
    blockOut = 16,
    blockIn = 16,
    inpBits = 8,
    wgtBits = 8,
    uopBits = 32,
    accBits = 32,
    outBits = 8,
    uopMemDepth = 1024,
    inpMemDepth = 2048,
    wgtMemDepth = 1024,
    accMemDepth = 2048,
    outMemDepth = 2048,
    instQueueEntries = 512)
})
