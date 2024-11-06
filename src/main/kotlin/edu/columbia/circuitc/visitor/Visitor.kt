package edu.columbia.circuitc.visitor

import edu.columbia.circuitc.parser.*

/**
 * Visitor pattern.
 */
interface Visitor {
    fun visit(circuitExpression: CircuitExpression)
    fun visit(argListExpression: ArgListExpression)
    fun visit(statementListExpression: StatementListExpression)
    fun visit(argumentExpression: ArgumentExpression)
    fun visit(clockDeclExpression: ClockDeclExpression)
    fun visit(assignmentExpression: AssignmentExpression)
    fun visit(registerDeclExpression: RegisterDeclExpression)
    fun visit(wireDeclExpression: WireDeclExpression)
    fun visit(unaryOpExpression: UnaryOpExpression)
    fun visit(binOpExpression: BinOpExpression)
    fun visit(ternaryOpExpression: TernaryOpExpression)
    fun visit(numericalOperand: NumericalOperand)
    fun visit(identifierOperand: IdentifierOperand)
    fun visit(operandListExpression: OperandListExpression)
}
