package edu.columbia.circuitc.lexer

/**
 * Lexer with constituent DFA rules ranked in order of highest (first) to lowest (last) priority.
 */
class Lexer {
    private val stateMachines: List<DFA> = listOf(
        // Keywords
        DFALoader.get("and")  { text, start, end -> Token(TokenType.AND, text, start, end) },
        DFALoader.get("or")   { text, start, end -> Token(TokenType.OR, text, start, end)  },
        DFALoader.get("not")  { text, start, end -> Token(TokenType.NOT, text, start, end) },
        DFALoader.get("xor")  { text, start, end -> Token(TokenType.XOR, text, start, end) },
        DFALoader.get("in")   { text, start, end -> Token(TokenType.IN, text, start, end)  },
        DFALoader.get("out")  { text, start, end -> Token(TokenType.OUT, text, start, end) },
        DFALoader.get("bits") { text, start, end -> Token(TokenType.BITS, text, start, end) },
        DFALoader.get("clock") { text, start, end -> Token(TokenType.CLOCK, text, start, end) },
        DFALoader.get("register") { text, start, end -> Token(TokenType.REGISTER, text, start, end) },
        DFALoader.get("circuit") { text, start, end -> Token(TokenType.CIRCUIT, text, start, end) },

        // Symbols
        DFALoader.get("<") { text, start, end -> Token(TokenType.LEFT_ANGLE, text, start, end) },
        DFALoader.get(">") { text, start, end -> Token(TokenType.RIGHT_ANGLE, text, start, end) },
        DFALoader.get("=") { text, start, end -> Token(TokenType.EQUALS, text, start, end) },
        DFALoader.get("{") { text, start, end -> Token(TokenType.LEFT_BRACE, text, start, end) },
        DFALoader.get("}") { text, start, end -> Token(TokenType.RIGHT_BRACE, text, start, end) },
        DFALoader.get("(") { text, start, end -> Token(TokenType.LEFT_PAREN, text, start, end) },
        DFALoader.get(")") { text, start, end -> Token(TokenType.RIGHT_PAREN, text, start, end) },
        DFALoader.get(";") { text, start, end -> Token(TokenType.SEMICOLON, text, start, end) },
        DFALoader.get(",") { text, start, end -> Token(TokenType.COMMA, text, start, end) },
        DFALoader.get("?") { text, start, end -> Token(TokenType.QUESTION, text, start, end) },
        DFALoader.get(":") { text, start, end -> Token(TokenType.COLON, text, start, end) },

        // Identifiers/Numbers
        DFALoader.get("id")      { text, start, end -> Token(TokenType.IDENTIFIER, text, start, end) },
        DFALoader.get("num") { text, start, end -> Token(TokenType.INT, text, start, end) },

        // Whitespace
        DFALoader.get("whitespace") { text, start, end -> Token(TokenType.IGNORED, text, start, end) },
    )

    fun tokenize(text: String): List<Token> {
        var lineNo = 1
        var columnNo = 1

        val tokens = mutableListOf<Token>()
        var start = TokenPos(lineNo, columnNo)

        for ((i, c) in text.toCharArray().withIndex()) {
            var valid = false
            val acceptable = mutableListOf<DFA>()

            for (machine in stateMachines) {
                val res = machine.consume(c)
                valid = valid || res

                if (machine.isAccept()) {
                    acceptable.add(machine)
                }
            }

            if (!valid) {
                error("$lineNo:$columnNo: invalid token: $c")
            }

            if (c == '\n') {
                lineNo++
                columnNo = 1
            } else {
                columnNo++
            }

            if (acceptable.isNotEmpty()) {
                val maximal = acceptable.filter {
                    i + 1 < text.toCharArray().size && it.peek(text.toCharArray()[i + 1])
                }

                val machine = if (maximal.isEmpty()) {
                    acceptable[0]
                } else {
                    null
                }

                machine?.let {
                    // State machine is in an accept state and cannot maximal munch
                    val token = machine.accept(start, TokenPos(lineNo, columnNo))
                    tokens.add(token)

                    stateMachines.forEach { it.reset() }
                    start = TokenPos(lineNo, columnNo)
                }
            }
        }

        return filterIgnored(tokens)
    }

    private fun filterIgnored(tokens: List<Token>): List<Token> {
        return tokens.filter {
            it.type != TokenType.IGNORED
        }
    }
}
