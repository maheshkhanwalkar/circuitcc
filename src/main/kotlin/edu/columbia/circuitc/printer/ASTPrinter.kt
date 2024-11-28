package edu.columbia.circuitc.printer

import edu.columbia.circuitc.parser.*
import edu.columbia.circuitc.visitor.ASTVisitor

/**
 * AST Printer.
 *
 * Leverages the visitor pattern to traverse through the AST and print out the
 * nodes.
 */
class ASTPrinter : ASTVisitor<Unit> {
    private var indent = 0

    override fun visit(circuitExpression: CircuitExpression) {
        println("CircuitExpression")
        printIndented("name: ${circuitExpression.name}")
        printIndented("args: ArgumentListExpression")

        indentScope {
            circuitExpression.args.accept(this)
        }

        printIndented("statements: StatementListExpression")

        indentScope {
            circuitExpression.statements.accept(this)
        }
    }

    override fun visit(argListExpression: ArgListExpression) {
        argListExpression.args.withIndex().forEach {
            printIndented("arg${it.index}: Argument")

            indentScope {
                it.value.accept(this)
            }
        }
    }

    override fun visit(argumentExpression: ArgumentExpression) {
        if (argumentExpression.isInput) {
            printIndented("type: INPUT")
        } else {
            printIndented("type: OUTPUT")
        }

        printIndented("name: ${argumentExpression.name}")
        printIndented("${argumentExpression.bitWidth} bits wide")
    }

    override fun visit(statementListExpression: StatementListExpression) {
        statementListExpression.statements.withIndex().forEach {
            printIndented("statement${it.index}: ${it.value.javaClass.simpleName}")

            indentScope {
                it.value.accept(this)
            }
        }
    }

    override fun visit(clockDeclExpression: ClockDeclExpression) {
        printIndented("name: ${clockDeclExpression.name}")
    }

    override fun visit(assignmentExpression: AssignmentExpression) {
        printIndented("lhs: ${assignmentExpression.lVal.javaClass.simpleName}")

        indentScope {
            (assignmentExpression.lVal as Expression).accept(this)
        }

        printIndented("rhs: ${assignmentExpression.rVal.javaClass.simpleName}")

        indentScope {
            assignmentExpression.rVal.accept(this)
        }
    }

    override fun visit(registerDeclExpression: RegisterDeclExpression) {
        printIndented("name: ${registerDeclExpression.name}")
        printIndented("${registerDeclExpression.bitWidth} bits wide")
        printIndented("params: ${registerDeclExpression.params.javaClass.simpleName}")

        indentScope {
            registerDeclExpression.params.accept(this)
        }
    }

    override fun visit(wireDeclExpression: WireDeclExpression) {
        printIndented("name: ${wireDeclExpression.name}")
        printIndented("${wireDeclExpression.bitWidth} bits wide")
    }

    override fun visit(unaryOpExpression: UnaryOpExpression) {
        printIndented("op: ${unaryOpExpression.type}")
        printIndented("rhs: ${unaryOpExpression.rhs.javaClass.simpleName}")

        indentScope {
            unaryOpExpression.rhs.accept(this)
        }
    }

    override fun visit(binOpExpression: BinOpExpression) {
        printIndented("op: ${binOpExpression.type}")
        printIndented("lhs: ${binOpExpression.lhs.javaClass.simpleName}")

        indentScope {
            binOpExpression.lhs.accept(this)
        }

        printIndented("rhs: ${binOpExpression.rhs.javaClass.simpleName}")

        indentScope {
            binOpExpression.rhs.accept(this)
        }
    }

    override fun visit(ternaryOpExpression: TernaryOpExpression) {
        printIndented("condition: ${ternaryOpExpression.condition.javaClass.simpleName}")

        indentScope {
            ternaryOpExpression.condition.accept(this)
        }

        printIndented("trueOp: ${ternaryOpExpression.trueOp.javaClass.simpleName}")

        indentScope {
            ternaryOpExpression.trueOp.accept(this)
        }

        printIndented("falseOp: ${ternaryOpExpression.falseOp.javaClass.simpleName}")

        indentScope {
            ternaryOpExpression.falseOp.accept(this)
        }
    }

    override fun visit(operandListExpression: OperandListExpression) {
        operandListExpression.operands.withIndex().forEach {
            printIndented("arg${it.index}: ${it.value.javaClass.simpleName}")

            indentScope {
                it.value.accept(this)
            }
        }
    }

    override fun visit(identifierOperand: IdentifierOperand) {
        printIndented("ID(${identifierOperand.name})")
    }

    override fun visit(numericalOperand: NumericalOperand) {
        printIndented("NUM(${numericalOperand.value})")
    }

    private fun printIndented(msg: String) {
        val spacing = " ".repeat(indent * 4)

        println("$spacing|__ $msg")
    }

    private fun indentScope(block: () -> Unit) {
        indent++
        block.invoke()
        indent--
    }
}

fun printAST(expression: Expression) {
    expression.accept(ASTPrinter())
}
