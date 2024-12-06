package edu.columbia.circuitc.opt

import edu.columbia.circuitc.ir.*

fun optimizeIR(ir: IRValue): IRValue {
    val optPasses = arrayOf(::copyPropagation, ::constantFolding, ::deadCodeElimination)
    var optIR = ir

    optPasses.forEach {
        optIR = it(optIR)
    }

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

private fun constantFolding(ir: IRValue): IRValue {
    return ir.accept(ConstantFolding())
}

private fun copyPropagation(ir: IRValue): IRValue {
    return ir.accept(CopyPropagation())
}
