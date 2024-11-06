package edu.columbia.circuitc.parser

import edu.columbia.circuitc.visitor.Visitor

interface Expression {
    fun accept(visitor: Visitor)
}

/**
 * Circuit expression, which is the top-level construct of the language.
 */
data class CircuitExpression(val name: String, val args: ArgListExpression, val statements: StatementListExpression) : Expression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * Argument list expression.
 */
data class ArgListExpression(val args: List<ArgumentExpression>) : Expression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * Argument expression.
 */
data class ArgumentExpression(val bitWidth: Int, val name: String, val isInput: Boolean) : Expression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * Statement list expression.
 */
data class StatementListExpression(val statements: List<StatementExpression>) : Expression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * Statement expression.
 */
interface StatementExpression : Expression

/**
 * L-val expression, an expression that appears on the left-hand side.
 */
interface LValExpression: Expression

/**
 * R-val expression, an expression that appears on the right-hand side.
 */
interface RValExpression: Expression

/**
 * Operand of an expression.
 */
interface Operand: RValExpression

/**
 * Operand list expression.
 */
data class OperandListExpression(val operands: List<Operand>) : Expression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

data class NumericalOperand(val value: Int): Operand {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
data class IdentifierOperand(val name: String): Operand, LValExpression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

enum class UnaryOp {
    NOT
}

enum class BinOp {
    AND, OR, XOR
}

/**
 * Unary operand expression. OP RHS
 */
data class UnaryOpExpression(val rhs: Operand, val type: UnaryOp): RValExpression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * Binary operand expression. LHS OP RHS
 */
data class BinOpExpression(val lhs: Operand, val rhs: Operand, val type: BinOp): RValExpression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * Ternary operand expression. COND ? TRUE-OP : FALSE-OP
 */
data class TernaryOpExpression(val condition: Operand, val trueOp: Operand, val falseOp: Operand) : RValExpression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * Declaration expression -- declares a variable. It can be a lval of an assignment.
 */
interface DeclExpression: LValExpression

/**
 * Wire declaration expression.
 */
data class WireDeclExpression(val bitWidth: Int, val name: String) : DeclExpression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * Clock declaration expression.
 */
data class ClockDeclExpression(val name: String) : DeclExpression, StatementExpression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * Register declaration expression.
 */
data class RegisterDeclExpression(
    val bitWidth: Int, val name: String, val params: OperandListExpression) : DeclExpression, StatementExpression {
        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

/**
 * Assignment expression. LVAL = RVAL
 */
data class AssignmentExpression(val lVal: LValExpression, val rVal: RValExpression): StatementExpression {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
