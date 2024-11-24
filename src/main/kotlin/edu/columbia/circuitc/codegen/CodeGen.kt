package edu.columbia.circuitc.codegen

import edu.columbia.circuitc.codegen.sim.*
import edu.columbia.circuitc.ir.*
import edu.columbia.circuitc.parser.*
import edu.columbia.circuitc.sym.SymbolTable
import edu.columbia.circuitc.visitor.ASTVisitor
import edu.columbia.circuitc.visitor.IRVisitor
import kotlin.math.abs

/**
 * Intermediate Representation (IR) Generator.
 *
 * This class implements a pass over the AST, performing "instruction lowering" to generate IR
 * to be used in later phases of the compiler. This IR is much closer to the actual circuit
 * constructs that exist in the SIM language, which makes codegen phase easier.
 */
class IRGen(private val symTable: SymbolTable<IRValue>): ASTVisitor<IRValue> {
    override fun visit(circuitExpression: CircuitExpression): IRValue {
        val name = circuitExpression.name

        // Generate IR for arguments and statements and merge together
        val argsList = circuitExpression.args.args.map { it.accept(this) }
        val statementsList = circuitExpression.statements.statements.map { it.accept(this) }

        return CircuitValue(name, (argsList + statementsList).toSet().toList())
    }

    override fun visit(argListExpression: ArgListExpression): IRValue {
        throw UnsupportedOperationException("IR generation not supported on argument list expression")
    }

    override fun visit(statementListExpression: StatementListExpression): IRValue {
        throw UnsupportedOperationException("IR generation not supported on statement list expression")
    }

    override fun visit(argumentExpression: ArgumentExpression): IRValue {
        if (argumentExpression.isInput) {
            // We don't know the actual value yet -- this will be updated in the assignment expression
            val input = InputPinValue(argumentExpression.name, argumentExpression.bitWidth, 0)
            symTable.put(input.pinName, input)

            return input
        } else {
            // Assign a dummy value to output for now -- this will be updated in the assignment expression
            val output = OutputPinValue(argumentExpression.name, argumentExpression.bitWidth,
                ConstantValue(1, 0))
            symTable.put(output.pinName, output)

            return output
        }
    }

    override fun visit(clockDeclExpression: ClockDeclExpression): IRValue {
        val clk = ClockValue(clockDeclExpression.name)
        symTable.put(clockDeclExpression.name, clk)

        return clk
    }

    override fun visit(assignmentExpression: AssignmentExpression): IRValue {
        val lVal = assignmentExpression.lVal.accept(this)
        val rVal = assignmentExpression.rVal.accept(this)

        if (lVal is InputPinValue) {
            lVal.inValue = (rVal as ConstantValue).value
        }
        if (lVal is OutputPinValue) {
            lVal.outValue = rVal

            if (rVal is ConstantValue) {
                rVal.bitWidth = lVal.bitWidth
            }
        }
        if (lVal is TunnelValue) {
            lVal.inValue = rVal

            if (rVal is ConstantValue) {
                rVal.bitWidth = lVal.bitWidth
            }
        }

        return lVal
    }

    override fun visit(registerDeclExpression: RegisterDeclExpression): IRValue {
        val paramValues = registerDeclExpression.params.operands.map { it.accept(this) }

        val clk = paramValues[0] as ClockValue
        val input = paramValues[1]
        val setBit = paramValues[2]
        val clearBit = paramValues[3]

        val reg = RegisterValue(registerDeclExpression.name, registerDeclExpression.bitWidth, input, setBit, clearBit, clk)
        symTable.put(reg.regName, reg)

        return reg
    }

    override fun visit(wireDeclExpression: WireDeclExpression): IRValue {
        val tunnel = TunnelValue(wireDeclExpression.name, wireDeclExpression.bitWidth, ConstantValue(1, 0))
        symTable.put(tunnel.tunnelName, tunnel)

        return tunnel
    }

    override fun visit(unaryOpExpression: UnaryOpExpression): IRValue {
        val inA = unaryOpExpression.rhs.accept(this)
        return NotGateValue(inA)
    }

    override fun visit(binOpExpression: BinOpExpression): IRValue {
        val inA = binOpExpression.lhs.accept(this)
        val inB = binOpExpression.rhs.accept(this)

        return when(binOpExpression.type) {
            BinOp.OR -> OrGateValue(inA, inB)
            BinOp.AND -> AndGateValue(inA, inB)
            BinOp.XOR -> XorGateValue(inA, inB)
        }
    }

    override fun visit(ternaryOpExpression: TernaryOpExpression): IRValue {
        val in0 = ternaryOpExpression.trueOp.accept(this)
        val in1 = ternaryOpExpression.falseOp.accept(this)
        val sel = ternaryOpExpression.condition.accept(this)

        return SelectorValue(in0, in1, sel)
    }

    override fun visit(numericalOperand: NumericalOperand): IRValue {
        // Bit-width will be adjusted based off of how this operand is used
        return ConstantValue(1, numericalOperand.value)
    }

    override fun visit(identifierOperand: IdentifierOperand): IRValue {
        val id = symTable.get(identifierOperand.name)

        if (id != null) {
            return id
        }

        throw IllegalArgumentException("unknown identifier: ${identifierOperand.name}")
    }

    override fun visit(operandListExpression: OperandListExpression): IRValue {
        throw UnsupportedOperationException("IR generation not supported on operand list expression")
    }
}

private const val PIN_NAME = "com.ra4king.circuitsim.gui.peers.wiring.PinPeer"
private const val TUNNEL_NAME = "com.ra4king.circuitsim.gui.peers.wiring.Tunnel"
private const val CLOCK_NAME = "com.ra4king.circuitsim.gui.peers.wiring.ClockPeer"
private const val REGISTER_NAME = "com.ra4king.circuitsim.gui.peers.memory.RegisterPeer"
private const val CONSTANT_NAME = "com.ra4king.circuitsim.gui.peers.wiring.ConstantPeer"
private const val AND_NAME = "com.ra4king.circuitsim.gui.peers.gates.AndGatePeer"
private const val OR_NAME = "com.ra4king.circuitsim.gui.peers.gates.OrGatePeer"
private const val XOR_NAME = "com.ra4king.circuitsim.gui.peers.gates.XorGatePeer"
private const val NOT_NAME = "com.ra4king.circuitsim.gui.peers.gates.NotGatePeer"
private const val SELECTOR_NAME = "com.ra4king.circuitsim.gui.peers.plexers.MultiplexerPeer"

/**
 * Code generation.
 *
 * This class implements a pass over the IR and generates SIM code (in object form) which can
 * be serialized to disk (in JSON form).
 */
class CodeGen(private val symTable: SymbolTable<SimConstruct>, private val builder: WireBuilder): IRVisitor<SimConstruct> {
    private val constructs = mutableListOf<SimConstruct>()

    private var xPos = 10
    private var yPos = 10

    override fun visit(circuitValue: CircuitValue): SimConstruct {
        circuitValue.values.map { it.accept(this) }
        return SimContainer(listOf(SimCircuit(circuitValue.name, constructs, builder.wires)))
    }

    override fun visit(inputPinValue: InputPinValue): SimConstruct {
        val existing = symTable.get(inputPinValue.pinName)

        if (existing != null) {
            return existing
        }

        val pos = getPos()

        val inputPinComponent = SimComponent(PIN_NAME, pos.first, pos.second, mapOf(
            "Label location" to "WEST",
            "Label" to inputPinValue.pinName,
            "Is input?" to "Yes",
            "Direction" to "EAST",
            "Bitsize" to inputPinValue.bitWidth.toString()
        ), listOf(), listOf(WirePoint(pos.first + 2, pos.second + 1, PointOrientation.EAST)))

        symTable.put(inputPinValue.pinName, inputPinComponent)
        constructs.add(inputPinComponent)

        return inputPinComponent
    }

    override fun visit(outputPinValue: OutputPinValue): SimConstruct {
        val input = outputPinValue.outValue.accept(this)
        val pos = getPos()

        val outputPinComponent = SimComponent(PIN_NAME, pos.first, pos.second, mapOf(
            "Label location" to "EAST",
            "Label" to outputPinValue.pinName,
            "Is input?" to "No",
            "Direction" to "WEST",
            "Bitsize" to outputPinValue.bitWidth.toString()
        ), listOf(WirePoint(pos.first, pos.second + 1, PointOrientation.WEST)), listOf())

        symTable.put(outputPinValue.pinName, outputPinComponent)
        builder.connect((input as SimComponent).outPosition[0], outputPinComponent.inPositions[0])

        constructs.add(outputPinComponent)
        return outputPinComponent
    }

    override fun visit(registerValue: RegisterValue): SimConstruct {
        val existing = symTable.get(registerValue.regName)

        if (existing != null) {
            return existing
        }

        val clkComponent = registerValue.clk.accept(this) as SimComponent
        val setBitComponent = registerValue.setBit.accept(this) as SimComponent
        val clearBitComponent = registerValue.clearBit.accept(this) as SimComponent
        val inComponent = registerValue.in0.accept(this) as SimComponent

        // TODO -- need to connect the wiring
        val pos = getPos()

        val regComponent = SimComponent(REGISTER_NAME, pos.first, pos.second, mapOf(
            "Label location" to "NORTH",
            "Label" to registerValue.regName,
            "Bitsize" to registerValue.bitWidth.toString()
        ), listOf(
            WirePoint(pos.first, pos.second + 2, PointOrientation.EAST),
            WirePoint(pos.first, pos.second + 3, PointOrientation.EAST),
            WirePoint(pos.first + 1, pos.second + 4, PointOrientation.EAST),
            WirePoint(pos.first + 3, pos.second + 4, PointOrientation.EAST)
        ), listOf(
            WirePoint(pos.first + 4, pos.second + 2, PointOrientation.EAST)
        ))

        builder.connect(inComponent.outPosition[0], regComponent.inPositions[0])
        builder.connect(setBitComponent.outPosition[0], regComponent.inPositions[1])
        builder.connect(clkComponent.outPosition[0],
            WirePoint(regComponent.inPositions[2].x, regComponent.inPositions[2].y + 2, regComponent.inPositions[2].orientation))
        builder.connect(regComponent.inPositions[2], WirePoint(regComponent.inPositions[2].x,
            regComponent.inPositions[2].y + 2, regComponent.inPositions[2].orientation)
        )

        builder.connect(clearBitComponent.outPosition[0],  WirePoint(regComponent.inPositions[3].x, regComponent.inPositions[3].y + 1, regComponent.inPositions[3].orientation))
        builder.connect(regComponent.inPositions[3], WirePoint(regComponent.inPositions[3].x,
            regComponent.inPositions[3].y + 1, regComponent.inPositions[3].orientation)
        )

        symTable.put(registerValue.regName, regComponent)
        constructs.add(regComponent)

        return regComponent
    }

    override fun visit(clkValue: ClockValue): SimConstruct {
        val existing = symTable.get(clkValue.clkName)

        if (existing != null) {
            return existing
        }

        val pos = getPos()

        val clkComponent = SimComponent(CLOCK_NAME, pos.first, pos.second, mapOf(
            "Label location" to "NORTH",
            "Label" to clkValue.clkName,
            "Direction" to "EAST"
        ), listOf(), listOf(WirePoint(pos.first + 2, pos.second + 1, PointOrientation.EAST)))

        symTable.put(clkValue.clkName, clkComponent)
        constructs.add(clkComponent)

        return clkComponent
    }

    override fun visit(selectorValue: SelectorValue): SimConstruct {
        val pos = getPos()

        val in0 = selectorValue.in0.accept(this) as SimComponent
        val in1 = selectorValue.in1.accept(this) as SimComponent
        val sel = selectorValue.sel.accept(this) as SimComponent

        val selectorComponent = SimComponent(SELECTOR_NAME, pos.first, pos.second, mapOf(
            "Selector location" to "Right/Down",
            "Label location" to "NORTH",
            "Selector bits" to "1",
            "Label" to "",
            "Direction" to "EAST",
            "Bitsize" to in0.properties["Bitsize"]!!
        ), listOf(
            WirePoint(pos.first, pos.second + 1, PointOrientation.EAST),
            WirePoint(pos.first, pos.second + 2, PointOrientation.EAST),
            WirePoint(pos.first + 1, pos.second + 4, PointOrientation.EAST),
        ), listOf(
            WirePoint(pos.first + 3, pos.second + 2, PointOrientation.EAST),
        ))

        builder.connect(in0.outPosition[0], selectorComponent.inPositions[1])
        builder.connect(in1.outPosition[0], selectorComponent.inPositions[0])
        builder.connect(sel.outPosition[0], selectorComponent.inPositions[2])

        constructs.add(selectorComponent)
        return selectorComponent
    }

    override fun visit(constantValue: ConstantValue): SimConstruct {
        val pos = getPos()

        val constant = SimComponent(CONSTANT_NAME, pos.first, pos.second, mapOf(
            "Label location" to "NORTH",
            "Label" to "",
            "Value" to constantValue.value.toString(),
            "Direction" to "EAST",
            "Bitsize" to constantValue.bitWidth.toString(),
            "Base" to "BINARY"
        ), listOf(), listOf(WirePoint(pos.first + 2, pos.second + 1, PointOrientation.EAST)))

        constructs.add(constant)
        return constant
    }

    override fun visit(tunnelValue: TunnelValue): SimConstruct {
        val existing = symTable.get(tunnelValue.tunnelName)

        if (existing != null) {
            return existing
        }

        val input = tunnelValue.inValue.accept(this)
        val pos = getPos()

        val tunnelComponent = SimComponent(TUNNEL_NAME, pos.first, pos.second, mapOf(
            "Label location" to "EAST",
            "Label" to tunnelValue.tunnelName,
            "Direction" to "WEST",
            "Bitsize" to tunnelValue.bitWidth.toString()
        ), listOf(WirePoint(pos.first, pos.second + 1, PointOrientation.WEST)),
            listOf(WirePoint(pos.first, pos.second + 1, PointOrientation.WEST)))

        symTable.put(tunnelValue.tunnelName, tunnelComponent)
        builder.connect((input as SimComponent).outPosition[0], tunnelComponent.inPositions[0])

        constructs.add(tunnelComponent)
        return tunnelComponent
    }

    override fun visit(andGateValue: AndGateValue): SimConstruct {
        val inA = andGateValue.inA.accept(this) as SimComponent
        val inB = andGateValue.inB.accept(this) as SimComponent

        val pos = getPos()
        val andComponent = SimComponent(AND_NAME, pos.first, pos.second, mapOf(
            "Negate 1" to "No",
            "Label location" to "NORTH",
            "Negate 0" to "No",
            "Number of Inputs" to "2",
            "Label" to "",
            "Direction" to "EAST",
            "Bitsize" to inA.properties["Bitsize"]!!
        ), listOf(WirePoint(pos.first, pos.second + 1, PointOrientation.EAST), WirePoint(pos.first, pos.second + 3, PointOrientation.EAST)),
            listOf(WirePoint(pos.first + 4, pos.second + 2, PointOrientation.EAST)))

        builder.connect(inA.outPosition[0], andComponent.inPositions[0])
        builder.connect(inB.outPosition[0], andComponent.inPositions[1])

        constructs.add(andComponent)
        return andComponent
    }

    override fun visit(orGateValue: OrGateValue): SimConstruct {
        val inA = orGateValue.inA.accept(this) as SimComponent
        val inB = orGateValue.inB.accept(this) as SimComponent

        val pos = getPos()
        val orComponent = SimComponent(OR_NAME, pos.first, pos.second, mapOf(
            "Negate 1" to "No",
            "Label location" to "NORTH",
            "Negate 0" to "No",
            "Number of Inputs" to "2",
            "Label" to "",
            "Direction" to "EAST",
            "Bitsize" to inA.properties["Bitsize"]!!
        ), listOf(WirePoint(pos.first, pos.second + 1, PointOrientation.EAST), WirePoint(pos.first, pos.second + 3, PointOrientation.EAST)),
            listOf(WirePoint(pos.first + 4, pos.second + 2, PointOrientation.EAST)))

        builder.connect(inA.outPosition[0], orComponent.inPositions[0])
        builder.connect(inB.outPosition[0], orComponent.inPositions[1])

        constructs.add(orComponent)
        return orComponent
    }

    override fun visit(xorGateValue: XorGateValue): SimConstruct {
        val inA = xorGateValue.inA.accept(this) as SimComponent
        val inB = xorGateValue.inB.accept(this) as SimComponent

        val pos = getPos()
        val xorComponent = SimComponent(XOR_NAME, pos.first, pos.second, mapOf(
            "Negate 1" to "No",
            "Label location" to "NORTH",
            "Negate 0" to "No",
            "Number of Inputs" to "2",
            "Label" to "",
            "Direction" to "EAST",
            "Bitsize" to inA.properties["Bitsize"]!!
        ), listOf(WirePoint(pos.first, pos.second + 1, PointOrientation.EAST), WirePoint(pos.first, pos.second + 3, PointOrientation.EAST)),
            listOf(WirePoint(pos.first + 4, pos.second + 2, PointOrientation.EAST)))

        builder.connect(inA.outPosition[0], xorComponent.inPositions[0])
        builder.connect(inB.outPosition[0], xorComponent.inPositions[1])

        constructs.add(xorComponent)
        return xorComponent
    }

    override fun visit(notGateValue: NotGateValue): SimConstruct {
        val inA = notGateValue.inA.accept(this) as SimComponent

        val pos = getPos()
        val notComponent = SimComponent(NOT_NAME, pos.first, pos.second, mapOf(
            "Label location" to "NORTH",
            "Label" to "",
            "Direction" to "EAST",
            "Bitsize" to inA.properties["Bitsize"]!!
        ), listOf(WirePoint(pos.first, pos.second + 1, PointOrientation.EAST)),
            listOf(WirePoint(pos.first + 3, pos.second + 1, PointOrientation.EAST)))

        builder.connect(inA.outPosition[0], notComponent.inPositions[0])

        constructs.add(notComponent)
        return notComponent
    }

    private fun getPos(): Pair<Int, Int> {
        val pos = xPos to yPos

        xPos += 5
        yPos += 5

        return pos
    }
}

class WireBuilder(val wires: MutableList<SimWire>) {
    fun connect(source: WirePoint, sink: WirePoint) {
        val xDist = abs(sink.x - source.x)
        val yDist = abs(sink.y - source.y)

        if (yDist != 0) {
            wires.add(SimWire(source.x, source.y, yDist, false))
        }

        if (xDist != 0) {
            wires.add(SimWire(source.x, sink.y, xDist, true))
        }
    }
}

fun generateIR(ast: Expression): IRValue {
    return ast.accept(IRGen(SymbolTable()))
}

fun generateSIMCode(ir: IRValue): SimContainer {
    val construct = ir.accept(CodeGen(SymbolTable(), WireBuilder(mutableListOf())))
    return construct as SimContainer
}
