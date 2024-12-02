package edu.columbia.circuitc.opt

import edu.columbia.circuitc.ir.*

fun optimizeIR(ir: IRValue): IRValue {
    val optIR = deadCodeElimination(ir)
    return optIR
}

private fun deadCodeElimination(ir: IRValue): IRValue {
    var optIR = ir
    var nextPassIR = ir.accept(DeadCodeElimination())

    while (nextPassIR != null) {
        optIR = nextPassIR
        nextPassIR = optIR.accept(DeadCodeElimination())
    }

    return optIR
}
