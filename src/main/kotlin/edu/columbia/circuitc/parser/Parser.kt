package edu.columbia.circuitc.parser

import edu.columbia.circuitc.lexer.Token
import edu.columbia.circuitc.lexer.TokenType
import edu.columbia.circuitc.printer.PrettyPrinter
import kotlin.system.exitProcess

class Parser(private val printer: PrettyPrinter) {
    fun parse(tokens: List<Token>): Expression {
        return parseProgram(tokens)
    }

    private fun parseProgram(tokens: List<Token>): CircuitExpression {
        var currTokens = tokens

        // PROG -> CIRCUIT ID '(' ARG-LIST ')' '{' STMT-LIST '}'
        val expectedLeftStructure = listOf(
            TokenType.CIRCUIT,
            TokenType.IDENTIFIER,
            TokenType.LEFT_PAREN
        )

        val expectedRightStructure = listOf(
            TokenType.RIGHT_PAREN,
            TokenType.LEFT_BRACE,
        )

        validateStructure(currTokens, expectedLeftStructure)

        val circuitName = currTokens[1].text
        val (argList, nTokens) = parseArgList(currTokens.subList(expectedLeftStructure.size, currTokens.size))

        currTokens = nTokens

        validateStructure(currTokens, expectedRightStructure)

        currTokens = currTokens.subList(expectedRightStructure.size, currTokens.size)
        val (stmtList, remTokens) = parseStatementList(currTokens)

        currTokens = remTokens
        validateStructure(currTokens, listOf(TokenType.RIGHT_BRACE))

        return CircuitExpression(circuitName, argList, stmtList)
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
        // ARG -> IN  BITS '<' NUM '>' ID |
        //        OUT BITS '<' NUM '>' ID
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

        // COMMA-ARG-LIST -> ',' ARG COMMA-ARG-LIST
        if (tokens[0].type != TokenType.COMMA) {
            unexpectedToken(tokens[0], TokenType.COMMA.text)
        }

        val (arg, nTokens) = parseArg(tokens.subList(1, tokens.size))
        val (argList, remTokens) = parseCommaArgList(nTokens)
        return listOf(arg) + argList to remTokens
    }

    private fun parseStatementList(tokens: List<Token>): Pair<StatementListExpression, List<Token>> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_BRACE)
        }

        // STMT-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_BRACE) {
            return StatementListExpression(emptyList()) to tokens
        }

        // STMT-LIST -> STMT SEMI-STMT-LIST
        val (arg, nTokens) = parseStatement(tokens)
        val (argList, remTokens) = parseSemiStatementList(nTokens)

        val allArgs = mutableListOf(arg) + argList
        return StatementListExpression(allArgs) to remTokens
    }

    private fun parseStatement(tokens: List<Token>): Pair<StatementExpression, List<Token>> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.IDENTIFIER)
        }

        // STMT -> LVAL | LVAL '=' RVAL
        val (lVal: LValExpression, currTokens) = when(tokens[0].type) {
            // LVAL -> BITS '<' NUM '>' ID
            TokenType.BITS -> {
                val expectedStructure = listOf(
                    TokenType.BITS,
                    TokenType.LEFT_ANGLE,
                    TokenType.NUM,
                    TokenType.RIGHT_ANGLE,
                    TokenType.IDENTIFIER
                )

                validateStructure(tokens, expectedStructure)
                WireDeclExpression(tokens[2].text.toInt(), tokens[4].text) to tokens.subList(expectedStructure.size, tokens.size)
            }

            // LVAL -> CLOCK ID
            TokenType.CLOCK -> {
                val expectedStructure = listOf(TokenType.CLOCK, TokenType.IDENTIFIER)
                validateStructure(tokens, expectedStructure)

                ClockDeclExpression(tokens[1].text) to tokens.subList(expectedStructure.size, tokens.size)
            }

            // LVAL -> REGISTER '<' NUM '>' ID '(' OP-LIST ')'
            TokenType.REGISTER -> {
                val expectedStructure = listOf(
                    TokenType.REGISTER, TokenType.LEFT_ANGLE, TokenType.NUM, TokenType.RIGHT_ANGLE,
                    TokenType.IDENTIFIER, TokenType.LEFT_PAREN
                )
                validateStructure(tokens, expectedStructure)

                val (opList, nTokens) = parseOperandList(tokens.subList(expectedStructure.size, tokens.size))

                validateStructure(nTokens, listOf(TokenType.RIGHT_PAREN))
                RegisterDeclExpression(tokens[2].text.toInt(), tokens[4].text, opList) to nTokens.subList(1, nTokens.size)
            }

            // LVAL -> ID
            TokenType.IDENTIFIER -> {
                val (op, nTokens) = parseOperand(tokens)

                if (op !is LValExpression) {
                    unexpectedToken(tokens[0], TokenType.IDENTIFIER.text)
                    throw Exception()
                }

                op to nTokens
            }

            else -> {
                unexpectedToken(tokens[0], TokenType.IDENTIFIER.text)
                throw Exception() // placate compiler
            }
        }

        // These decl expressions don't allow assignment, so just return them
        if (lVal is StatementExpression) {
            return lVal as StatementExpression to currTokens
        }

        // '=' RVAL
        validateStructure(currTokens, listOf(TokenType.EQUALS))
        val (rVal, remTokens) = parseRValExpression(currTokens.subList(1, currTokens.size))

        return AssignmentExpression(lVal, rVal) to remTokens
    }

    private fun parseSemiStatementList(tokens: List<Token>): Pair<List<StatementExpression>, List<Token>> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_BRACE)
        }

        // SEMI-STMT-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_BRACE) {
            return emptyList<StatementExpression>() to tokens
        }

        if (tokens[0].type != TokenType.SEMICOLON) {
            unexpectedToken(tokens[0], TokenType.SEMICOLON.text)
        }

        // SEMI-STMT-LIST -> ';' [FOLLOW(SEMI-STMT-LIST) = {'}'} => no more statements in the list]
        if (tokens[1].type == TokenType.RIGHT_BRACE) {
            return emptyList<StatementExpression>() to tokens.subList(1, tokens.size)
        }

        // SEMI-STMT-LIST -> ';' STMT SEMI-STMT-LIST
        val (arg, nTokens) = parseStatement(tokens.subList(1, tokens.size))
        val (argList, remTokens) = parseSemiStatementList(nTokens)
        return listOf(arg) + argList to remTokens
    }

    private fun parseOperandList(tokens: List<Token>): Pair<OperandListExpression, List<Token>> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_PAREN)
        }

        // OP-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_PAREN) {
            return OperandListExpression(emptyList()) to tokens
        }

        // OP-LIST -> OP COMMA-OP-LIST
        val (arg, nTokens) = parseOperand(tokens)
        val (argList, remTokens) = parseCommaOpList(nTokens)

        return OperandListExpression(listOf(arg) + argList) to remTokens
    }

    private fun parseOperand(tokens: List<Token>): Pair<Operand, List<Token>> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.IDENTIFIER)
        }

        // OP -> ID | NUM
        return when (tokens[0].type) {
            TokenType.IDENTIFIER -> IdentifierOperand(tokens[0].text) to tokens.subList(1, tokens.size)
            TokenType.NUM -> NumericalOperand(tokens[0].text.toInt()) to tokens.subList(1, tokens.size)
            else -> {
                unexpectedToken(tokens[0], or(TokenType.IDENTIFIER.text, TokenType.NUM.text))
                throw Exception() // placate compiler
            }
        }
    }

    private fun parseCommaOpList(tokens: List<Token>): Pair<List<Operand>, List<Token>> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_PAREN)
        }

        // COMMA-OP-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_PAREN) {
            return emptyList<Operand>() to tokens
        }

        // COMMA-OP-LIST -> ',' OP COMMA-OP-LIST
        if (tokens[0].type != TokenType.COMMA) {
            unexpectedToken(tokens[0], TokenType.COMMA.text)
        }

        val (arg, nTokens) = parseOperand(tokens.subList(1, tokens.size))
        val (argList, remTokens) = parseCommaOpList(nTokens)
        return listOf(arg) + argList to remTokens
    }

    private fun parseRValExpression(tokens: List<Token>): Pair<RValExpression, List<Token>> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.IDENTIFIER)
        }

        if (tokens[0].type == TokenType.NOT) {
            // RVAL -> NOT OP
            val (op, nTokens) = parseOperand(tokens.subList(1, tokens.size))
            return UnaryOpExpression(op, UnaryOp.NOT) to nTokens
        } else {
            val (op, nTokens) = parseOperand(tokens)

            if (nTokens.isEmpty()) {
                unexpectedEOF(TokenType.SEMICOLON)
            }

            // RVAL -> OP
            if (nTokens[0].type == TokenType.SEMICOLON) {
                return op to nTokens
            }

            return when(nTokens[0].type) {
                // RVAL -> OP BIN-OP OP; BIN-OP -> AND | OR | XOR
                TokenType.AND, TokenType.OR, TokenType.XOR -> {
                    val (rhs, remTokens) = parseOperand(nTokens.subList(1, nTokens.size))
                    BinOpExpression(op, rhs, toBinOpType(nTokens[0].type)) to remTokens
                }

                // RVAL -> OP '?' OP ':' OP
                TokenType.QUESTION -> {
                    val (trueOp, remTokens) = parseOperand(nTokens.subList(1, nTokens.size))

                    if (remTokens.isEmpty()) {
                        unexpectedEOF(TokenType.COLON)
                    }

                    if (remTokens[0].type != TokenType.COLON) {
                        unexpectedToken(remTokens[0], TokenType.COLON.text)
                    }

                    val (falseOp, remTokensLeft) = parseOperand(remTokens.subList(1, remTokens.size))
                    TernaryOpExpression(op, trueOp, falseOp) to remTokensLeft
                }

                else -> {
                    unexpectedToken(nTokens[0], or(TokenType.AND.text,
                        TokenType.OR.text, TokenType.XOR.text, TokenType.QUESTION.text))
                    throw Exception() // placate compiler
                }
            }
        }
    }

    private fun toBinOpType(tokenType: TokenType): BinOp {
        return when (tokenType) {
            TokenType.AND -> BinOp.AND
            TokenType.OR -> BinOp.OR
            TokenType.XOR -> BinOp.XOR
            else -> throw IllegalStateException("converting non-operator token to binary operator")
        }
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
        val message = "unexpected token '${token.text}', expected: '$expected'"
        printer.printMessage(message, token.start, token.end)
        exitProcess(0)
    }

    private fun or(vararg messages: String): String {
        return messages.joinToString("', or '")
    }
}
