package edu.columbia.circuitc.parser

import edu.columbia.circuitc.lexer.TokenPos
import edu.columbia.circuitc.visitor.ASTVisitor

data class ExpressionBounds(val start: TokenPos, val end: TokenPos)

abstract class Expression(open val bounds: ExpressionBounds) {
    abstract fun <T> accept(visitor: ASTVisitor<T>): T
}

/**
 * Circuit expression, which is the top-level construct of the language.
 */
data class CircuitExpression(val name: String, val args: ArgListExpression, val statements: StatementListExpression,
                             override val bounds: ExpressionBounds) : Expression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Argument list expression.
 */
data class ArgListExpression(val args: List<ArgumentExpression>,
                             override val bounds: ExpressionBounds) : Expression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Argument expression.
 */
data class ArgumentExpression(val bitWidth: Int, val name: String, val isInput: Boolean,
                              override val bounds: ExpressionBounds) : Expression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Statement list expression.
 */
data class StatementListExpression(val statements: List<StatementExpression>,
                                   override val bounds: ExpressionBounds) : Expression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Statement expression.
 */
abstract class StatementExpression(override val bounds: ExpressionBounds) : Expression(bounds)

/**
 * L-val expression, an expression that appears on the left-hand side.
 */
interface LValExpression

/**
 * R-val expression, an expression that appears on the right-hand side.
 */
abstract class RValExpression(override val bounds: ExpressionBounds): Expression(bounds)

/**
 * Operand of an expression.
 */
abstract class Operand(override val bounds: ExpressionBounds): RValExpression(bounds)

/**
 * Operand list expression.
 */
data class OperandListExpression(val operands: List<Operand>, override val bounds: ExpressionBounds) :
    Expression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

data class NumericalOperand(val value: Int, override val bounds: ExpressionBounds): Operand(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

data class IdentifierOperand(val name: String, override val bounds: ExpressionBounds): Operand(bounds), LValExpression {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
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
data class UnaryOpExpression(val rhs: Operand, val type: UnaryOp, override val bounds: ExpressionBounds):
    RValExpression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Binary operand expression. LHS OP RHS
 */
data class BinOpExpression(val lhs: Operand, val rhs: Operand, val type: BinOp, override val bounds: ExpressionBounds):
    RValExpression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Ternary operand expression. COND ? TRUE-OP : FALSE-OP
 */
data class TernaryOpExpression(val condition: Operand, val trueOp: Operand, val falseOp: Operand,
                               override val bounds: ExpressionBounds) : RValExpression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Declaration expression -- declares a variable. It can be a lval of an assignment.
 */
interface DeclExpression: LValExpression

/**
 * Wire declaration expression.
 */
data class WireDeclExpression(val bitWidth: Int, val name: String,
                              override val bounds: ExpressionBounds) : DeclExpression, Expression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Clock declaration expression.
 */
data class ClockDeclExpression(val name: String, override val bounds: ExpressionBounds) : DeclExpression,
    StatementExpression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Register declaration expression.
 */
data class RegisterDeclExpression(val bitWidth: Int, val name: String, val params: OperandListExpression,
                                  override val bounds: ExpressionBounds) : DeclExpression, StatementExpression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}

/**
 * Assignment expression. LVAL = RVAL
 */
data class AssignmentExpression(val lVal: LValExpression, val rVal: RValExpression,
                                override val bounds: ExpressionBounds): StatementExpression(bounds) {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)
}
