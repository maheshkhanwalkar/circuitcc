package edu.columbia.circuitc.parser

interface Expression

/**
 * Circuit expression, which is the top-level construct of the language.
 */
data class CircuitExpression(private val name: String, private val args: ArgListExpression) : Expression

/**
 * Argument list expression.
 */
data class ArgListExpression(val args: List<ArgumentExpression>) : Expression

/**
 * Argument expression.
 */
data class ArgumentExpression(private val bitWidth: Int, private val name: String, private val isInput: Boolean) : Expression
