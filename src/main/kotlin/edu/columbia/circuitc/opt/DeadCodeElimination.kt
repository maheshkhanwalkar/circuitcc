package edu.columbia.circuitc.opt

import edu.columbia.circuitc.ir.*
import edu.columbia.circuitc.sym.SymbolTable
import edu.columbia.circuitc.visitor.IRVisitor

/**
 * Dead code elimination optimization.
 *
 * This optimization pass scans through the IR and identifies any declarations
 * with no usages and removes them.
 *
 * It is very likely that this optimization pass needs to be run multiple times
 * to eliminate all the dead-code. This is because one pass' deletions can lead
 * to additional dead code, hence we will need another pass to eliminate those.
 *
 * The visit(...) method on CircuitValue (top-level IR construct) will return a
 * new CircuitValue when it eliminates dead-code. If it cannot eliminate anything,
 * it returns null -- so that can be used as a stop-condition on repeated passes.
 */
class DeadCodeElimination: IRVisitor<IRValue?> {
    private val refCount = mutableMapOf<String, Int>()
    private val symTable = SymbolTable<IRValue>()

    override fun visit(circuitValue: CircuitValue): IRValue? {
        circuitValue.values.forEach { it.accept(this) }
        val deadSet = mutableSetOf<IRValue>()

        for (ref in refCount.keys) {
            val count = refCount[ref]

            // Dead code construct
            if (count == 0) {
                val irValue = symTable.get(ref)!!

                // Output pins won't have any use-references, since they are assign-only
                if (irValue is OutputPinValue) {
                    continue
                }

                println("Eliminating unused declaration: '$ref'")
                deadSet.add(irValue)
            }
        }

        return if (deadSet.isNotEmpty()) {
            val nValues = circuitValue.values.filter { it !in deadSet }
            CircuitValue(circuitValue.name, nValues)
        } else {
            null
        }
    }

    override fun visit(inputPinValue: InputPinValue): IRValue? {
        insertOrIncrement(inputPinValue.pinName, inputPinValue)
        return null
    }

    override fun visit(outputPinValue: OutputPinValue): IRValue? {
        insertOrIncrement(outputPinValue.pinName, outputPinValue)
        outputPinValue.outValue.accept(this)
        return null
    }

    override fun visit(registerValue: RegisterValue): IRValue? {
        insertOrIncrement(registerValue.regName, registerValue)

        registerValue.in0.accept(this)
        registerValue.clk.accept(this)
        registerValue.setBit.accept(this)
        registerValue.clearBit.accept(this)

        return null
    }

    override fun visit(clkValue: ClockValue): IRValue? {
        insertOrIncrement(clkValue.clkName, clkValue)
        return null
    }

    override fun visit(selectorValue: SelectorValue): IRValue? {
        selectorValue.in0.accept(this)
        selectorValue.in1.accept(this)
        selectorValue.sel.accept(this)
        return null
    }

    override fun visit(constantValue: ConstantValue): IRValue? {
        return null
    }

    override fun visit(tunnelValue: TunnelValue): IRValue? {
        insertOrIncrement(tunnelValue.tunnelName, tunnelValue)
        tunnelValue.inValue.accept(this)
        return null
    }

    override fun visit(andGateValue: AndGateValue): IRValue? {
        andGateValue.inA.accept(this)
        andGateValue.inB.accept(this)
        return null
    }

    override fun visit(orGateValue: OrGateValue): IRValue? {
        orGateValue.inA.accept(this)
        orGateValue.inB.accept(this)
        return null
    }

    override fun visit(xorGateValue: XorGateValue): IRValue? {
        xorGateValue.inA.accept(this)
        xorGateValue.inB.accept(this)
        return null
    }

    override fun visit(notGateValue: NotGateValue): IRValue? {
        notGateValue.inA.accept(this)
        return null
    }

    private fun insertOrIncrement(name: String, value: IRValue) {
        if (name !in refCount) {
            symTable.put(name, value)
            refCount[name] = 0
        } else {
            refCount[name] = refCount[name]!! + 1
        }
    }
}