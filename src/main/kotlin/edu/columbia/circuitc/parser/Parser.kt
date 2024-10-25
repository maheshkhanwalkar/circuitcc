package edu.columbia.circuitc.parser

import edu.columbia.circuitc.lexer.Token
import edu.columbia.circuitc.lexer.TokenType

class Parser {
    fun parse(tokens: List<Token>): Expression {
        return parseProgram(tokens)
    }

    private fun parseProgram(tokens: List<Token>): CircuitExpression {
        var currTokens = tokens

        // PROG -> CIRCUIT ID '(' ARG-LIST ')' '{' '}'
        val expectedLeftStructure = listOf(
            TokenType.CIRCUIT,
            TokenType.IDENTIFIER,
            TokenType.LEFT_PAREN
        )

        val expectedRightStructure = listOf(
            TokenType.RIGHT_PAREN,
            TokenType.LEFT_BRACE,
            TokenType.RIGHT_BRACE
        )

        validateStructure(currTokens, expectedLeftStructure)

        val circuitName = currTokens[1].text
        val (argList, nTokens) = parseArgList(currTokens.subList(expectedLeftStructure.size, currTokens.size))

        currTokens = nTokens

        validateStructure(currTokens, expectedRightStructure)
        return CircuitExpression(circuitName, argList)
    }

    private fun parseArgList(tokens: List<Token>): Pair<ArgListExpression, List<Token>> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_PAREN)
        }

        // ARG-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_PAREN) {
            return ArgListExpression(emptyList()) to tokens
        }

        // ARG-LIST -> ARG COMMA-ARG-LIST
        val (arg, nTokens) = parseArg(tokens)
        val (argList, remTokens) = parseCommaArgList(nTokens)

        val allArgs = mutableListOf(arg) + argList
        return ArgListExpression(allArgs) to remTokens
    }

    private fun parseArg(tokens: List<Token>): Pair<ArgumentExpression, List<Token>> {
        val expectedStructure = listOf(
            TokenType.BITS,
            TokenType.LEFT_ANGLE,
            TokenType.NUM,
            TokenType.RIGHT_ANGLE,
            TokenType.IDENTIFIER
        )

        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.IN)
        }

        val isInput = when (tokens[0].type) {
            TokenType.IN -> true
            TokenType.OUT -> false
            else -> {
                unexpectedToken(tokens[0], TokenType.IN.text)
                throw Exception() // placate compiler
            }
        }

        var currTokens = tokens.subList(1, tokens.size)
        validateStructure(currTokens, expectedStructure)

        val bitWidth = currTokens[2].text.toInt()
        val name = currTokens[4].text

        currTokens = currTokens.subList(expectedStructure.size, currTokens.size)
        return ArgumentExpression(bitWidth, name, isInput) to currTokens
    }

    private fun parseCommaArgList(tokens: List<Token>): Pair<List<ArgumentExpression>, List<Token>> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_PAREN)
        }

        // COMMA-ARG-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_PAREN) {
            return emptyList<ArgumentExpression>() to tokens
        }

        // COMMA-ARG-LIST -> ',' ARG ARG-LIST
        if (tokens[0].type != TokenType.COMMA) {
            unexpectedToken(tokens[0], TokenType.COMMA.text)
        }

        val (arg, nTokens) = parseArg(tokens.subList(1, tokens.size))
        val (argList, remTokens) = parseArgList(nTokens)
        return listOf(arg) + argList.args to remTokens
    }

    private fun validateStructure(tokens: List<Token>, expectedStructure: List<TokenType>) {
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
    }

    private fun unexpectedEOF(tokenType: TokenType) {
        println("unexpected EOF, expected: ${tokenType.text}")
    }

    private fun unexpectedToken(token: Token, expected: String) {
        println("(${token.start.row}, ${token.start.col}): unexpected token '${token.text}', expected: '$expected'")
        throw IllegalStateException("unexpected token")
    }
}
