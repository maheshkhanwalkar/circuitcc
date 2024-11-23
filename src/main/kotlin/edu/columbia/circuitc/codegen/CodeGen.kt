package edu.columbia.circuitc.codegen

import edu.columbia.circuitc.ir.*
import edu.columbia.circuitc.parser.*
import edu.columbia.circuitc.sym.SymbolTable
import edu.columbia.circuitc.visitor.ASTVisitor

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

fun generateIR(ast: Expression): IRValue {
    return ast.accept(IRGen(SymbolTable()))
}
