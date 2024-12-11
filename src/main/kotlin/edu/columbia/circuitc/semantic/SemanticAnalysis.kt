package edu.columbia.circuitc.semantic

import edu.columbia.circuitc.parser.*
import edu.columbia.circuitc.printer.PrettyPrinter
import edu.columbia.circuitc.sym.SymbolTable
import edu.columbia.circuitc.visitor.ASTVisitor
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.max

/**
 * Bit-width representation.
 *
 * @param width bit width
 * @param adjustable whether the width is flexible -- it can change according to some target.
 */
data class BitWidth(val width: Int, val adjustable: Boolean)

private val DummyBitWidth = BitWidth(0, false)

/**
 * Bit-width verification.
 *
 * Ensure that bit-widths match for assignments and that constant values can be encoded within
 * the specified bit-width that is inferred.
 */
class BitWidthVerification(private val printer: PrettyPrinter): ASTVisitor<BitWidth> {
    var successful = true
    private set

    private val symTable: SymbolTable<BitWidth> = SymbolTable()

    override fun visit(circuitExpression: CircuitExpression): BitWidth {
        circuitExpression.args.accept(this)
        circuitExpression.statements.accept(this)
        return DummyBitWidth
    }

    override fun visit(argListExpression: ArgListExpression): BitWidth {
        argListExpression.args.forEach { it.accept(this) }
        return DummyBitWidth
    }

    override fun visit(statementListExpression: StatementListExpression): BitWidth {
        statementListExpression.statements.forEach { it.accept(this) }
        return DummyBitWidth
    }

    override fun visit(argumentExpression: ArgumentExpression): BitWidth {
        val bitWidth = BitWidth(argumentExpression.bitWidth, false)
        symTable.put(argumentExpression.name, bitWidth)
        return bitWidth
    }

    override fun visit(clockDeclExpression: ClockDeclExpression): BitWidth {
        return DummyBitWidth
    }

    override fun visit(assignmentExpression: AssignmentExpression): BitWidth {
        val lValWidth = (assignmentExpression.lVal as Expression).accept(this)
        val rValWidth = assignmentExpression.rVal.accept(this)

        // Widths need to match or rValWidth is < lValWidth and is adjustable
        if (lValWidth != rValWidth && (!rValWidth.adjustable || rValWidth.width > lValWidth.width)) {
            val lVal = assignmentExpression.lVal as Expression
            val rVal = assignmentExpression.rVal

            printer.printMessage("bit width mismatch: lval=${lValWidth.width}, rval=${rValWidth.width}",
                lVal.bounds, rVal.bounds)

            successful = false
            return lValWidth
        }

        return lValWidth
    }

    override fun visit(registerDeclExpression: RegisterDeclExpression): BitWidth {
        val operands = registerDeclExpression.params.operands

        if (operands.size != 4) {
            printer.printMessage("incorrect number of operands, expected 4 but got ${operands.size}",
                registerDeclExpression.params.bounds)
            successful = false
        } else {
            if (operands[0] as? IdentifierOperand == null) {
                printer.printMessage("first parameter should be an identifier", operands[0].bounds)
                successful = false
            }

            val inputWidth = operands[1].accept(this)
            val (_, succ) = binaryWidthComparison(BitWidth(registerDeclExpression.bitWidth, false), inputWidth)

            if (!succ) {
                printer.printMessage("bit width mismatch between register declaration and input: " +
                        "decl=${registerDeclExpression.bitWidth}, input=${inputWidth.width}",
                    registerDeclExpression.bounds, operands[1].bounds)
            }

            val setBitWidth = operands[2].accept(this)
            val clearBitWidth = operands[3].accept(this)

            if (setBitWidth.width != 1) {
                printer.printMessage("set-bit should be 1 bit wide, but is actually: ${setBitWidth.width}",
                    operands[2].bounds)
                successful = false
            }

            if (clearBitWidth.width != 1) {
                printer.printMessage("clear-bit should be 1 bit wide, but is actually: ${clearBitWidth.width}",
                    operands[3].bounds)
                successful = false
            }
        }

        val bitWidth = BitWidth(registerDeclExpression.bitWidth, false)
        symTable.put(registerDeclExpression.name, bitWidth)
        return bitWidth
    }

    override fun visit(wireDeclExpression: WireDeclExpression): BitWidth {
        val bitWidth = BitWidth(wireDeclExpression.bitWidth, false)
        symTable.put(wireDeclExpression.name, bitWidth)
        return bitWidth
    }

    override fun visit(unaryOpExpression: UnaryOpExpression): BitWidth {
        return unaryOpExpression.rhs.accept(this)
    }

    override fun visit(binOpExpression: BinOpExpression): BitWidth {
        val lhsWidth = binOpExpression.lhs.accept(this)
        val rhsWidth = binOpExpression.rhs.accept(this)

        val (width, succ) = binaryWidthComparison(lhsWidth, rhsWidth)

        if (!succ) {
            printer.printMessage("bit width mismatch between lhs and rhs: lhs=${lhsWidth.width}, rhs=${rhsWidth.width}",
                binOpExpression.lhs.bounds,
                binOpExpression.rhs.bounds)
        }

        return width
    }

    override fun visit(ternaryOpExpression: TernaryOpExpression): BitWidth {
        val trueOpWidth = ternaryOpExpression.trueOp.accept(this)
        val falseOpWidth = ternaryOpExpression.falseOp.accept(this)
        val condWidth = ternaryOpExpression.condition.accept(this)

        if (condWidth.width != 1) {
            printer.printMessage("ternary condition should be 1-bit wide", ternaryOpExpression.condition.bounds)
            successful = false
        }

        val (width, succ) = binaryWidthComparison(trueOpWidth, falseOpWidth)

        if (!succ) {
            printer.printMessage(
                "bit-width mismatch between true and false operands: trueOp=${trueOpWidth.width}, falseOp=${falseOpWidth.width}",
                ternaryOpExpression.trueOp.bounds, ternaryOpExpression.falseOp.bounds)
        }

        return width
    }

    override fun visit(numericalOperand: NumericalOperand): BitWidth {
        // Numerical operands' width can be adjusted upwards
        if (numericalOperand.value == 0) {
            return BitWidth(1, true)
        }

        val minWidth = (floor(log(numericalOperand.value.toDouble(), 2.0)) + 1).toInt()
        return BitWidth(minWidth, true)
    }

    override fun visit(identifierOperand: IdentifierOperand): BitWidth {
        val existing = symTable.get(identifierOperand.name)

        if (existing != null) {
            return existing
        }

        printer.printMessage("undefined identifier: ${identifierOperand.name}", identifierOperand.bounds)
        successful = false
        return DummyBitWidth
    }

    override fun visit(operandListExpression: OperandListExpression): BitWidth {
        return DummyBitWidth
    }

    private fun binaryWidthComparison(lhsWidth: BitWidth, rhsWidth: BitWidth): Pair<BitWidth, Boolean> {
        if (lhsWidth.width != rhsWidth.width) {
            if (lhsWidth.adjustable && rhsWidth.adjustable) {
                return BitWidth(max(lhsWidth.width, rhsWidth.width), true) to true
            }

            if (lhsWidth.adjustable && lhsWidth.width < rhsWidth.width) {
                return rhsWidth to true
            }

            if (rhsWidth.adjustable && rhsWidth.width < lhsWidth.width) {
                return lhsWidth to true
            }

            successful = false
            return lhsWidth to false
        } else {
            return lhsWidth to true
        }
    }
}

/**
 * Duplicate declaration verification.
 *
 * Validate that there are no duplicate declarations of variables within the AST,
 * which is a semantic error.
 */
class DuplicateDeclarationVerification(private val printer: PrettyPrinter): ASTVisitor<Unit> {
    private val symTable = SymbolTable<Expression>()

    var successful = true
    private set

    override fun visit(circuitExpression: CircuitExpression) {
        circuitExpression.args.accept(this)
        circuitExpression.statements.accept(this)
    }

    override fun visit(argListExpression: ArgListExpression) {
        argListExpression.args.forEach { it.accept(this) }
    }

    override fun visit(statementListExpression: StatementListExpression) {
        statementListExpression.statements.forEach { it.accept(this) }
    }

    override fun visit(argumentExpression: ArgumentExpression) {
        checkExisting(argumentExpression.name, argumentExpression)
    }

    override fun visit(clockDeclExpression: ClockDeclExpression) {
       checkExisting(clockDeclExpression.name, clockDeclExpression)
    }

    override fun visit(assignmentExpression: AssignmentExpression) {
        (assignmentExpression.lVal as Expression).accept(this)
        assignmentExpression.rVal.accept(this)
    }

    override fun visit(registerDeclExpression: RegisterDeclExpression) {
        checkExisting(registerDeclExpression.name, registerDeclExpression)
    }

    override fun visit(wireDeclExpression: WireDeclExpression) {
        checkExisting(wireDeclExpression.name, wireDeclExpression)
    }

    override fun visit(unaryOpExpression: UnaryOpExpression) {
        unaryOpExpression.rhs.accept(this)
    }

    override fun visit(binOpExpression: BinOpExpression) {
        binOpExpression.lhs.accept(this)
        binOpExpression.rhs.accept(this)
    }

    override fun visit(ternaryOpExpression: TernaryOpExpression) {
        ternaryOpExpression.trueOp.accept(this)
        ternaryOpExpression.falseOp.accept(this)
        ternaryOpExpression.condition.accept(this)
    }

    override fun visit(numericalOperand: NumericalOperand) { }
    override fun visit(identifierOperand: IdentifierOperand) { }
    override fun visit(operandListExpression: OperandListExpression) { }

    private fun checkExisting(name: String, expression: Expression) {
        val existing = symTable.get(name)

        if (existing != null) {
            printer.printMessage("redefinition of '$name' found", expression.bounds, existing.bounds)
            successful = false
            return
        }

        symTable.put(name, expression)
    }
}

fun performSemanticAnalysis(ast: Expression, printer: PrettyPrinter): Boolean {
    val duplicateDeclarationVerification = DuplicateDeclarationVerification(printer)
    ast.accept(duplicateDeclarationVerification)
    if (!duplicateDeclarationVerification.successful) {
        return false
    }

    val bitWidthVerification = BitWidthVerification(printer)
    ast.accept(bitWidthVerification)
    return bitWidthVerification.successful
}
