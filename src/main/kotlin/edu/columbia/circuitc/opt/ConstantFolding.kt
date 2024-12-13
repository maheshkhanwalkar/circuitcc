package edu.columbia.circuitc.opt

import edu.columbia.circuitc.ir.*
import edu.columbia.circuitc.visitor.IRVisitor

/**
 * Constant Folding optimization.
 *
 * This optimization pass analyses operand expressions that consist of constant values,
 * computes the result of the expression, and replaces the expression with the constant result.
 *
 * Original:
 *    bits<4> example = 2 or 1;
 *
 * Optimized:
 *    bits<4> example = 3; // 3 = 2 or 1 (bitwise or)
 */
class ConstantFolding: IRVisitor<IRValue> {
    override fun visit(circuitValue: CircuitValue): IRValue {
        val nValues = circuitValue.values.map { it.accept(this) }
        return CircuitValue(circuitValue.name, nValues)
    }

    override fun visit(inputPinValue: InputPinValue): IRValue {
        return inputPinValue
    }

    override fun visit(outputPinValue: OutputPinValue): IRValue {
        val nOutValue = outputPinValue.outValue.accept(this)
        adjustIfConstant(nOutValue, outputPinValue.bitWidth)
        return OutputPinValue(outputPinValue.pinName, outputPinValue.bitWidth, nOutValue)
    }

    override fun visit(registerValue: RegisterValue): IRValue {
        val nIn0 = registerValue.in0.accept(this)
        adjustIfConstant(nIn0, registerValue.bitWidth)
        return RegisterValue(registerValue.regName, registerValue.bitWidth, nIn0, registerValue.setBit,
            registerValue.clearBit, registerValue.clk)
    }

    override fun visit(clkValue: ClockValue): IRValue {
        return clkValue
    }

    override fun visit(selectorValue: SelectorValue): IRValue {
        return selectorValue
    }

    override fun visit(constantValue: ConstantValue): IRValue {
        return constantValue
    }

    override fun visit(tunnelValue: TunnelValue): IRValue {
        val nInValue =  tunnelValue.inValue.accept(this)
        adjustIfConstant(nInValue, tunnelValue.bitWidth)
        return TunnelValue(tunnelValue.tunnelName, tunnelValue.bitWidth, nInValue)
    }

    override fun visit(andGateValue: AndGateValue): IRValue {
        // Candidate for constant folding
        return if (andGateValue.inA is ConstantValue && andGateValue.inB is ConstantValue) {
            val inA = andGateValue.inA
            val inB = andGateValue.inB

            val result = inA.value and inB.value
            println("Folding ${inA.value} and ${inB.value} into $result")
            ConstantValue(inA.bitWidth, result)
        } else {
            andGateValue
        }
    }

    override fun visit(orGateValue: OrGateValue): IRValue {
        return if (orGateValue.inA is ConstantValue && orGateValue.inB is ConstantValue) {
            val inA = orGateValue.inA
            val inB = orGateValue.inB

            val result = inA.value or inB.value
            println("Folding ${inA.value} or ${inB.value} into $result")
            ConstantValue(inA.bitWidth, result)
        } else {
            orGateValue
        }
    }

    override fun visit(xorGateValue: XorGateValue): IRValue {
        return if (xorGateValue.inA is ConstantValue && xorGateValue.inB is ConstantValue) {
            val inA = xorGateValue.inA
            val inB = xorGateValue.inB

            val result = inA.value xor inB.value
            println("Folding ${inA.value} xor ${inB.value} into $result")
            ConstantValue(inA.bitWidth, result)
        } else {
            xorGateValue
        }
    }

    override fun visit(notGateValue: NotGateValue): IRValue {
        return if (notGateValue.inA is ConstantValue) {
            val inA = notGateValue.inA

            val result = inA.value.inv()
            println("Folding not ${inA.value} into $result")
            ConstantValue(inA.bitWidth, result)
        } else {
            notGateValue
        }
    }

    private fun adjustIfConstant(inValue: IRValue, bitWidth: Int) {
        if (inValue is ConstantValue) {
            inValue.bitWidth = bitWidth
        }
    }
}
