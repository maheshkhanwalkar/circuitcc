package edu.columbia.circuitc.parser

import edu.columbia.circuitc.lexer.Token
import edu.columbia.circuitc.lexer.TokenType

class Parser {
    fun parse(tokens: List<Token>): Expression {
        return parseProgram(tokens)
    }

    private fun parseProgram(tokens: List<Token>): CircuitExpression {
        // PROG -> CIRCUIT ID '(' ')' '{' '}'
        val expectedStructure = listOf(
            TokenType.CIRCUIT,
            TokenType.IDENTIFIER,
            TokenType.LEFT_PAREN,
            TokenType.RIGHT_PAREN,
            TokenType.LEFT_BRACE,
            TokenType.RIGHT_BRACE
        )

        // Ensure the token stream matches the expected grammatical structure
        for ((i, expectedToken) in expectedStructure.withIndex()) {
            if (i >= tokens.size) {
                unexpectedEOF(expectedToken)
            }

            val token = tokens[i]

            if (token.type != expectedToken) {
                unexpectedToken(token, expectedToken.text)
            }
        }

        val circuitName = tokens[1].text
        return CircuitExpression(circuitName)
    }

    private fun unexpectedEOF(tokenType: TokenType) {
        println("unexpected EOF, expected: ${tokenType.text}")
    }

    private fun unexpectedToken(token: Token, expected: String) {
        println("(${token.start.row}, ${token.start.col}): unexpected token '${token.text}', expected: '$expected'")
        throw IllegalStateException("unexpected token")
    }
}
