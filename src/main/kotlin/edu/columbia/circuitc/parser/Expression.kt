package edu.columbia.circuitc.parser

interface Expression

/**
 * Circuit expression, which is the top-level construct of the language.
 */
data class CircuitExpression(private val name: String, private val args: ArgListExpression,
                             private val statements: StatementListExpression) : Expression

/**
 * Argument list expression.
 */
data class ArgListExpression(val args: List<ArgumentExpression>) : Expression

/**
 * Argument expression.
 */
data class ArgumentExpression(private val bitWidth: Int, private val name: String, private val isInput: Boolean) : Expression

/**
 * Statement list expression.
 */
data class StatementListExpression(val statements: List<StatementExpression>) : Expression

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
data class OperandListExpression(val operands: List<Operand>) : Expression

data class NumericalOperand(private val value: Int): Operand
data class IdentifierOperand(private val name: String): Operand, LValExpression

enum class UnaryOp {
    NOT
}

enum class BinOp {
    AND, OR, XOR
}

/**
 * Unary operand expression. OP RHS
 */
data class UnaryOpExpression(private val rhs: Operand, private val type: UnaryOp): RValExpression

/**
 * Binary operand expression. LHS OP RHS
 */
data class BinOpExpression(private val lhs: Operand, private val rhs: Operand, private val type: BinOp): RValExpression

/**
 * Ternary operand expression. COND ? TRUE-OP : FALSE-OP
 */
data class TernaryOpExpression(private val condition: Operand, private val trueOp: Operand,
                               private val falseOp: Operand) : RValExpression

/**
 * Declaration expression -- declares a variable. It can be a lval of an assignment.
 */
interface DeclExpression: LValExpression

/**
 * Wire declaration expression.
 */
data class WireDeclExpression(private val bitWidth: Int, private val name: String) : DeclExpression

/**
 * Clock declaration expression.
 */
data class ClockDeclExpression(private val name: String) : DeclExpression, StatementExpression

/**
 * Register declaration expression.
 */
data class RegisterDeclExpression(
    private val bitWidth: Int,
    private val name: String, private val params: OperandListExpression) : DeclExpression, StatementExpression

/**
 * Assignment expression. LVAL = RVAL
 */
data class AssignmentExpression(private val lVal: LValExpression, private val rVal: RValExpression): StatementExpression
