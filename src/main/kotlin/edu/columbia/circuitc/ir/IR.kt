package edu.columbia.circuitc.ir

import edu.columbia.circuitc.visitor.IRVisitor

interface IRValue {
    fun <T> accept(visitor: IRVisitor<T>): T
}

/**
 * Circuit IR.
 */
data class CircuitValue(val name: String, val values: List<IRValue>) : IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * Constant IR value.
 */
data class ConstantValue(var bitWidth: Int, val value: Int): IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * Tunnel IR value. This represents a low-level tunnel (named wire) construct.
 */
data class TunnelValue(val tunnelName: String, val bitWidth: Int, var inValue: IRValue): IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * Input pin IR value.
 */
data class InputPinValue(val pinName: String, val bitWidth: Int, var inValue: Int): IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * Output pin IR value.
 */
data class OutputPinValue(val pinName: String, val bitWidth: Int, var outValue: IRValue): IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * Selector IR value. This represents a selector circuit construct.
 */
data class SelectorValue(val in0: IRValue, val in1: IRValue, val sel: IRValue) : IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * Register IR value. This represents a register circuit construct.
 */
data class RegisterValue(val regName: String, val bitWidth: Int, val in0: IRValue, val setBit: IRValue,
                         val clearBit: IRValue, val clk: ClockValue): IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * Clock IR value.
 */
data class ClockValue(val clkName: String): IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * OR Gate IR value.
 */
data class OrGateValue(val inA: IRValue, val inB: IRValue) : IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * AND Gate IR value.
 */
data class AndGateValue(val inA: IRValue, val inB: IRValue): IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * XOR Gate IR value.
 */
data class XorGateValue(val inA: IRValue, val inB: IRValue): IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}

/**
 * NOT Gate IR value.
 */
data class NotGateValue(val inA: IRValue): IRValue {
    override fun <T> accept(visitor: IRVisitor<T>): T = visitor.visit(this)
}
