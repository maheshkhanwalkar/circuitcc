package edu.columbia.circuitc.parser

interface Expression

/**
 * Circuit expression, which is the top-level construct of the language.
 */
data class CircuitExpression(private val name: String) : Expression
