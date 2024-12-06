package edu.columbia.circuitc.opt

import edu.columbia.circuitc.ir.*
import edu.columbia.circuitc.visitor.IRVisitor

/**
 * Copy propagation optimization.
 *
 * This optimization pass performs both constant propagation to replace
 * variable usages with constants and copy propagation to replace variable
 * usages with the earliest variable in the chain.
 *
 * Constant Propagation:
 *  Original:
 *    bits<4> a = 3;
 *    bits<4> b = a;
 *
 *  Optimized:
 *    bits<4> a = 3;
 *    bits<4> b = 3;
 *
 * Copy Propagation
 *  Original:
 *    bits<4> a = <...>
 *    bits<4> b = a;
 *    bits<4> c = b and d;
 *
 *  Optimized:
 *    bits<4> a = <...>
 *    bits<4> b = a;
 *    bits<4> c = a and d;
 *
 * This optimized tends to lead to dead-code, so it makes sense to run
 * this pass before dead-code elimination to ensure all the dead code
 * is removed.
 */
class CopyPropagation: IRVisitor<IRValue> {
    override fun visit(circuitValue: CircuitValue): IRValue {
        val nValues = circuitValue.values.map { it.accept(this) }
        return CircuitValue(circuitValue.name, nValues)
    }

    override fun visit(inputPinValue: InputPinValue): IRValue {
        return inputPinValue
    }

    override fun visit(outputPinValue: OutputPinValue): IRValue {
        val outVal = outputPinValue.outValue.accept(this)
        return OutputPinValue(outputPinValue.pinName, outputPinValue.bitWidth, outVal)
    }

    override fun visit(registerValue: RegisterValue): IRValue {
        val in0 = registerValue.in0.accept(this)
        val setBit = registerValue.setBit.accept(this)
        val clearBit = registerValue.clearBit.accept(this)

        return RegisterValue(registerValue.regName, registerValue.bitWidth, in0, setBit, clearBit, registerValue.clk)
    }

    override fun visit(clkValue: ClockValue): IRValue {
        return clkValue
    }

    override fun visit(selectorValue: SelectorValue): IRValue {
        val in0 = selectorValue.in0.accept(this)
        val in1 = selectorValue.in1.accept(this)
        val sel = selectorValue.sel.accept(this)

        return SelectorValue(in0, in1, sel)
    }

    override fun visit(constantValue: ConstantValue): IRValue {
        return constantValue
    }

    override fun visit(tunnelValue: TunnelValue): IRValue {
        return propagate(tunnelValue)
    }

    override fun visit(andGateValue: AndGateValue): IRValue {
        val inA = andGateValue.inA.accept(this)
        val inB = andGateValue.inB.accept(this)
        return AndGateValue(inA, inB)
    }

    override fun visit(orGateValue: OrGateValue): IRValue {
        val inA = orGateValue.inA.accept(this)
        val inB = orGateValue.inB.accept(this)
        return OrGateValue(inA, inB)
    }

    override fun visit(xorGateValue: XorGateValue): IRValue {
        val inA = xorGateValue.inA.accept(this)
        val inB = xorGateValue.inB.accept(this)
        return XorGateValue(inA, inB)
    }

    override fun visit(notGateValue: NotGateValue): IRValue {
        val inA = notGateValue.inA.accept(this)
        return NotGateValue(inA)
    }

    private fun propagate(irValue: IRValue): IRValue {
        return if (irValue is TunnelValue) {
            return if (irValue.inValue is TunnelValue || irValue.inValue is InputPinValue || irValue.inValue is ConstantValue) {
                // b = a; c = b; => c = a
                irValue.inValue
            } else {
                irValue
            }
        } else {
            irValue
        }
    }
}
