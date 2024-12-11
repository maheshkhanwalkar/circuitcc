package edu.columbia.circuitc.opt

import edu.columbia.circuitc.ir.*
import edu.columbia.circuitc.visitor.IRVisitor
import kotlin.math.pow

/**
 * Logic gate elimination.
 *
 * This optimization pass eliminates logic gates that are redundant due to truth-table evaluation on
 * constant values. For example, if a constant value is a (single) input to a logic gate and that value
 * itself determines the output, then we can just eliminate the gate altogether.
 *
 * Example:
 *
 *   bits<1> a = <...> // some non-const value
 *   bits<1> output = a or 1;
 *
 *   In this case, it doesn't matter what the value of 'a' is, the value assigned to 'output' will
 *   always be 1 since we are ORing with 1.
 *
 *   bits<1> a = <...> // some non-const value
 *   bits<1> output = a and 0;
 *
 *   Similarly, in this case, it also doesn't matter what 'a' is, 'output' will always be 0.
 *
 * For larger bit-widths, it becomes a bit more complicated -- as this is a bitwise operation, so we
 * need to check against all-bits-set or no-bits-set as the criteria for elimination.
 */
class LogicGateElimination: IRVisitor<IRValue> {
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
        return registerValue
    }

    override fun visit(clkValue: ClockValue): IRValue {
        return clkValue
    }

    override fun visit(selectorValue: SelectorValue): IRValue {
        val sel = selectorValue.sel

        // Can optimize out the selector if the sel bit is a constant -- since
        // we know for sure which input to propagate.
        return if (sel is ConstantValue) {
            if (sel.value == 1) {
                selectorValue.in1
            } else {
                selectorValue.in0
            }
        } else {
            selectorValue
        }
    }

    override fun visit(constantValue: ConstantValue): IRValue {
        return constantValue
    }

    override fun visit(tunnelValue: TunnelValue): IRValue {
        val nVal = tunnelValue.inValue.accept(this)
        return TunnelValue(tunnelValue.tunnelName, tunnelValue.bitWidth, nVal)
    }

    override fun visit(andGateValue: AndGateValue): IRValue {
        val (const, other) = getConstant(andGateValue.inA, andGateValue.inB)

        if (const == null) {
            return andGateValue
        }

        val allBits = generateAllOnes(const.bitWidth)

        // X and 111...1 = X
        if (const.value == allBits) {
            println("Eliminating AND gate, since one operand has all bits set to 1")
            return other
        }

        // X and 0 = 0
        if (const.value == 0) {
            println("Eliminating AND gate, since one operand is 0")
            return const
        }

        return andGateValue
    }

    override fun visit(orGateValue: OrGateValue): IRValue {
        val (const, other) = getConstant(orGateValue.inA, orGateValue.inB)

        if (const == null) {
            return orGateValue
        }

        // X or 0 = X
        if (const.value == 0) {
            println("Eliminating OR gate, since one operand is the constant 0")
            return other
        }

        val allBits = generateAllOnes(const.bitWidth)

        // X or 111...1 = 111...1
        if (const.value == allBits) {
            println("Eliminating OR gate, since one operand has all bits set to 1")
            return const
        }

        return orGateValue
    }

    override fun visit(xorGateValue: XorGateValue): IRValue {
        return xorGateValue
    }

    override fun visit(notGateValue: NotGateValue): IRValue {
        return notGateValue
    }

    private fun getConstant(inA: IRValue, inB: IRValue): Pair<ConstantValue?, IRValue> {
        if (inA is ConstantValue && inB is ConstantValue) {
            return null to inA
        }

        if (inA is ConstantValue) {
            return inA to inB
        }

        if (inB is ConstantValue) {
            return inB to inA
        }

        return null to inA
    }

    private fun generateAllOnes(bitWidth: Int): Int {
        return 2.0.pow(bitWidth).toInt() - 1
    }
}
