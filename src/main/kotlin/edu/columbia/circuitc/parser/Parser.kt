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
        val currTokens = tokens.toMutableList()

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
        val startPos = currTokens[0].start

        val circuitName = currTokens[1].text
        currTokens.removeAmount(expectedLeftStructure.size)

        val argList = parseArgList(currTokens)

        validateStructure(currTokens, expectedRightStructure)

        currTokens.removeAmount(expectedRightStructure.size)
        val stmtList = parseStatementList(currTokens)

        validateStructure(currTokens, listOf(TokenType.RIGHT_BRACE))
        val endPos = currTokens[0].end

        return CircuitExpression(circuitName, argList, stmtList, ExpressionBounds(startPos, endPos))
    }

    private fun parseArgList(tokens: MutableList<Token>): ArgListExpression {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_PAREN)
        }

        // ARG-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_PAREN) {
            val pos = tokens[0].start
            val bounds = ExpressionBounds(pos, pos)

            return ArgListExpression(emptyList(), bounds)
        }

        // ARG-LIST -> ARG COMMA-ARG-LIST
        val startPos = tokens[0].start
        val arg = parseArg(tokens)
        val argList = parseCommaArgList(tokens)

        val allArgs = mutableListOf(arg) + argList

        val endPos = argList.lastOrNull()?.bounds?.end ?: arg.bounds.end
        return ArgListExpression(allArgs, ExpressionBounds(startPos, endPos))
    }

    private fun parseArg(tokens: MutableList<Token>): ArgumentExpression {
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

        val startPos = tokens[0].start

        tokens.removeAmount(1)
        validateStructure(tokens, expectedStructure)

        val bitWidth = tokens[2].text.toInt()
        val name = tokens[4].text

        val endPos = tokens[4].end

        tokens.removeAmount(expectedStructure.size)
        return ArgumentExpression(bitWidth, name, isInput, ExpressionBounds(startPos, endPos))
    }

    private fun parseCommaArgList(tokens: MutableList<Token>): List<ArgumentExpression> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_PAREN)
        }

        // COMMA-ARG-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_PAREN) {
            return emptyList()
        }

        // COMMA-ARG-LIST -> ',' ARG COMMA-ARG-LIST
        if (tokens[0].type != TokenType.COMMA) {
            unexpectedToken(tokens[0], TokenType.COMMA.text)
        }

        tokens.removeAmount(1)
        val arg = parseArg(tokens)
        val argList = parseCommaArgList(tokens)

        return listOf(arg) + argList
    }

    private fun parseStatementList(tokens: MutableList<Token>): StatementListExpression {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_BRACE)
        }

        // STMT-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_BRACE) {
            val pos = tokens[0].start
            val bounds = ExpressionBounds(pos, pos)

            return StatementListExpression(emptyList(), bounds)
        }

        // STMT-LIST -> STMT SEMI-STMT-LIST
        val startPos = tokens[0].start
        val arg = parseStatement(tokens)
        val argList = parseSemiStatementList(tokens)

        val endPos = argList.lastOrNull()?.bounds?.end ?: arg.bounds.end

        val allArgs = mutableListOf(arg) + argList
        return StatementListExpression(allArgs, ExpressionBounds(startPos, endPos))
    }

    private fun parseStatement(tokens: MutableList<Token>): StatementExpression {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.IDENTIFIER)
        }

        // STMT -> LVAL | LVAL '=' RVAL
        val lVal: LValExpression = when(tokens[0].type) {
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
                val bitWidth = tokens[2].text.toInt()
                val name = tokens[4].text

                val startPos = tokens[0].start
                val endPos = tokens[expectedStructure.size - 1].end

                tokens.removeAmount(expectedStructure.size)
                WireDeclExpression(bitWidth, name, ExpressionBounds(startPos, endPos))
            }

            // LVAL -> CLOCK ID
            TokenType.CLOCK -> {
                val expectedStructure = listOf(TokenType.CLOCK, TokenType.IDENTIFIER)
                validateStructure(tokens, expectedStructure)

                val startPos = tokens[0].start
                val endPos = tokens[1].end

                val name = tokens[1].text
                tokens.removeAmount(expectedStructure.size)

                ClockDeclExpression(name, ExpressionBounds(startPos, endPos))
            }

            // LVAL -> REGISTER '<' NUM '>' ID '(' OP-LIST ')'
            TokenType.REGISTER -> {
                val expectedStructure = listOf(
                    TokenType.REGISTER, TokenType.LEFT_ANGLE, TokenType.NUM, TokenType.RIGHT_ANGLE,
                    TokenType.IDENTIFIER, TokenType.LEFT_PAREN
                )
                validateStructure(tokens, expectedStructure)

                val startPos = tokens[0].start

                val bitWidth = tokens[2].text.toInt()
                val name = tokens[4].text

                tokens.removeAmount(expectedStructure.size)
                val opList = parseOperandList(tokens)

                validateStructure(tokens, listOf(TokenType.RIGHT_PAREN))
                val endPos = tokens[0].end
                tokens.removeAmount(1)

                RegisterDeclExpression(bitWidth, name, opList, ExpressionBounds(startPos, endPos))
            }

            // LVAL -> ID
            TokenType.IDENTIFIER -> {
                val op = parseOperand(tokens)

                if (op !is LValExpression) {
                    unexpectedToken(tokens[0], TokenType.IDENTIFIER.text)
                    throw Exception()
                }

                op
            }

            else -> {
                unexpectedToken(tokens[0], TokenType.IDENTIFIER.text)
                throw Exception() // placate compiler
            }
        }

        // These decl expressions don't allow assignment, so just return them
        if (lVal is StatementExpression) {
            return lVal
        }

        // '=' RVAL
        validateStructure(tokens, listOf(TokenType.EQUALS))
        tokens.removeAmount(1)

        val rVal = parseRValExpression(tokens)
        return AssignmentExpression(lVal, rVal, ExpressionBounds((lVal as Expression).bounds.start, rVal.bounds.end))
    }

    private fun parseSemiStatementList(tokens: MutableList<Token>): List<StatementExpression> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_BRACE)
        }

        if (tokens[0].type != TokenType.SEMICOLON) {
            unexpectedToken(tokens[0], TokenType.SEMICOLON.text)
        }

        // SEMI-STMT-LIST -> ';' [FOLLOW(SEMI-STMT-LIST) = {'}'} => no more statements in the list]
        if (tokens[1].type == TokenType.RIGHT_BRACE) {
            tokens.removeAmount(1)
            return emptyList()
        }

        // SEMI-STMT-LIST -> ';' STMT SEMI-STMT-LIST
        tokens.removeAmount(1)
        val arg = parseStatement(tokens)

        val argList = parseSemiStatementList(tokens)
        return listOf(arg) + argList
    }

    private fun parseOperandList(tokens: MutableList<Token>): OperandListExpression {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_PAREN)
        }

        // OP-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_PAREN) {
            val pos = tokens[0].start
            val bounds = ExpressionBounds(pos, pos)

            return OperandListExpression(emptyList(), bounds)
        }

        // OP-LIST -> OP COMMA-OP-LIST
        val startPos = tokens[0].start
        val arg = parseOperand(tokens)
        val argList = parseCommaOpList(tokens)
        val endPos = argList.lastOrNull()?.bounds?.end ?: arg.bounds.end

        return OperandListExpression(listOf(arg) + argList, ExpressionBounds(startPos, endPos))
    }

    private fun parseOperand(tokens: MutableList<Token>): Operand {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.IDENTIFIER)
        }

        // OP -> ID | NUM
        val bounds = ExpressionBounds(tokens[0].start, tokens[0].end)
        val op = when (tokens[0].type) {
            TokenType.IDENTIFIER -> IdentifierOperand(tokens[0].text, bounds)
            TokenType.NUM -> NumericalOperand(tokens[0].text.toInt(), bounds)
            else -> {
                unexpectedToken(tokens[0], or(TokenType.IDENTIFIER.text, TokenType.NUM.text))
                throw Exception() // placate compiler
            }
        }

        tokens.removeAmount(1)
        return op
    }

    private fun parseCommaOpList(tokens: MutableList<Token>): List<Operand> {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.RIGHT_PAREN)
        }

        // COMMA-OP-LIST -> epsilon
        if (tokens[0].type == TokenType.RIGHT_PAREN) {
            return emptyList()
        }

        // COMMA-OP-LIST -> ',' OP COMMA-OP-LIST
        if (tokens[0].type != TokenType.COMMA) {
            unexpectedToken(tokens[0], TokenType.COMMA.text)
        }

        tokens.removeAmount(1)
        val arg = parseOperand(tokens)
        val argList = parseCommaOpList(tokens)
        return listOf(arg) + argList
    }

    private fun parseRValExpression(tokens: MutableList<Token>): RValExpression {
        if (tokens.isEmpty()) {
            unexpectedEOF(TokenType.IDENTIFIER)
        }

        if (tokens[0].type == TokenType.NOT) {
            val startPos = tokens[0].start

            // RVAL -> NOT OP
            tokens.removeAmount(1)
            val op = parseOperand(tokens)
            return UnaryOpExpression(op, UnaryOp.NOT, ExpressionBounds(startPos, op.bounds.end))
        } else {
            val op = parseOperand(tokens)

            if (tokens.isEmpty()) {
                unexpectedEOF(TokenType.SEMICOLON)
            }

            // RVAL -> OP
            if (tokens[0].type == TokenType.SEMICOLON) {
                return op
            }

            val tokType = tokens[0].type

            return when(tokType) {
                // RVAL -> OP BIN-OP OP; BIN-OP -> AND | OR | XOR
                TokenType.AND, TokenType.OR, TokenType.XOR -> {
                    tokens.removeAmount(1)
                    val rhs = parseOperand(tokens)
                    BinOpExpression(op, rhs, toBinOpType(tokType), ExpressionBounds(op.bounds.start, rhs.bounds.end))
                }

                // RVAL -> OP '?' OP ':' OP
                TokenType.QUESTION -> {
                    tokens.removeAmount(1)
                    val trueOp = parseOperand(tokens)

                    if (tokens.isEmpty()) {
                        unexpectedEOF(TokenType.COLON)
                    }

                    if (tokens[0].type != TokenType.COLON) {
                        unexpectedToken(tokens[0], TokenType.COLON.text)
                    }

                    tokens.removeAmount(1)
                    val falseOp = parseOperand(tokens)
                    TernaryOpExpression(op, trueOp, falseOp, ExpressionBounds(op.bounds.start, falseOp.bounds.end))
                }

                else -> {
                    unexpectedToken(tokens[0], or(TokenType.AND.text,
                        TokenType.OR.text, TokenType.XOR.text, TokenType.QUESTION.text, TokenType.SEMICOLON.text))
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

    private fun MutableList<Token>.removeAmount(count: Int) {
        (1..count).forEach { _ ->
            this.removeFirst()
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
