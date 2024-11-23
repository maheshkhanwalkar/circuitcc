package edu.columbia.circuitc.ir

interface IRValue

/**
 * Circuit IR.
 */
data class CircuitValue(val name: String, val values: List<IRValue>) : IRValue

/**
 * Constant IR value.
 */
data class ConstantValue(var bitWidth: Int, val value: Int): IRValue

/**
 * Tunnel IR value. This represents a low-level tunnel (named wire) construct.
 */
data class TunnelValue(val tunnelName: String, val bitWidth: Int, var inValue: IRValue): IRValue

/**
 * Input pin IR value.
 */
data class InputPinValue(val pinName: String, val bitWidth: Int, var inValue: Int): IRValue

/**
 * Output pin IR value.
 */
data class OutputPinValue(val pinName: String, val bitWidth: Int, var outValue: IRValue): IRValue

/**
 * Selector IR value. This represents a selector circuit construct.
 */
data class SelectorValue(val in0: IRValue, val in1: IRValue, val sel: IRValue) : IRValue

/**
 * Register IR value. This represents a register circuit construct.
 */
data class RegisterValue(val regName: String, val bitWidth: Int, val in0: IRValue, val setBit: IRValue,
                         val clearBit: IRValue, val clk: ClockValue): IRValue

/**
 * Clock IR value.
 */
data class ClockValue(val clkName: String): IRValue

/**
 * OR Gate IR value.
 */
data class OrGateValue(val inA: IRValue, val inB: IRValue) : IRValue

/**
 * AND Gate IR value.
 */
data class AndGateValue(val inA: IRValue, val inB: IRValue): IRValue

/**
 * XOR Gate IR value.
 */
data class XorGateValue(val inA: IRValue, val inB: IRValue): IRValue

/**
 * NOT Gate IR value.
 */
data class NotGateValue(val inA: IRValue): IRValue
