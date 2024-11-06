package edu.columbia.circuitc.visitor

import edu.columbia.circuitc.parser.*

/**
 * Visitor pattern.
 */
interface Visitor<T> {
    fun visit(circuitExpression: CircuitExpression): T
    fun visit(argListExpression: ArgListExpression): T
    fun visit(statementListExpression: StatementListExpression): T
    fun visit(argumentExpression: ArgumentExpression): T
    fun visit(clockDeclExpression: ClockDeclExpression): T
    fun visit(assignmentExpression: AssignmentExpression): T
    fun visit(registerDeclExpression: RegisterDeclExpression): T
    fun visit(wireDeclExpression: WireDeclExpression): T
    fun visit(unaryOpExpression: UnaryOpExpression): T
    fun visit(binOpExpression: BinOpExpression): T
    fun visit(ternaryOpExpression: TernaryOpExpression): T
    fun visit(numericalOperand: NumericalOperand): T
    fun visit(identifierOperand: IdentifierOperand): T
    fun visit(operandListExpression: OperandListExpression): T
}
