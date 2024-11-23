package edu.columbia.circuitc.visitor

import edu.columbia.circuitc.ir.*
import edu.columbia.circuitc.parser.*

/**
 * Visitor pattern for AST.
 */
interface ASTVisitor<T> {
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

/**
 * Visitor pattern for IR.
 */
interface IRVisitor<T> {
    fun visit(circuitValue: CircuitValue): T
    fun visit(inputPinValue: InputPinValue): T
    fun visit(outputPinValue: OutputPinValue): T
    fun visit(registerValue: RegisterValue): T
    fun visit(clkValue: ClockValue): T
    fun visit(selectorValue: SelectorValue): T
    fun visit(constantValue: ConstantValue): T
    fun visit(tunnelValue: TunnelValue): T
    fun visit(andGateValue: AndGateValue): T
    fun visit(orGateValue: OrGateValue): T
    fun visit(xorGateValue: XorGateValue): T
    fun visit(notGateValue: NotGateValue): T
}
