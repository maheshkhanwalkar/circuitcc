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
        DFALoader.get("num") { text, start, end -> Token(TokenType.NUM, text, start, end) },

        // Whitespace
        DFALoader.get("whitespace") { text, start, end -> Token(TokenType.IGNORED, text, start, end) },
    )

    fun tokenize(text: String): List<Token> {
        var lineNo = 1
        var columnNo = 1

        val tokens = mutableListOf<Token>()
        var start = TokenPos(lineNo, columnNo)

        var inComment = false

        fun handleError(c: Char) {
            // Report the error and keep on going
            println("error: $lineNo:$columnNo: invalid token: $c")
        }

        for ((i, c) in text.toCharArray().withIndex()) {
            var valid = false
            val acceptable = mutableListOf<DFA>()

            if (!inComment) {
                // Detect start of comment
                if (c == '/') {
                    if (i + 1 < text.length && text[i + 1] == '/') {
                        inComment = true
                    } else {
                        handleError(c)
                    }
                } else {
                    // Consume the character and tabulate all DFAs in an accept state
                    for (machine in stateMachines) {
                        val res = machine.consume(c)
                        valid = valid || res

                        if (machine.isAccept()) {
                            acceptable.add(machine)
                        }
                    }

                    // Invalid token -- all DFAs rejected it
                    if (!valid) {
                        handleError(c)
                    }
                }
            }

            if (c == '\n') {
                lineNo++
                columnNo = 1

                // Newline terminates the comment
                if (inComment) {
                    inComment = false
                    start = TokenPos(lineNo, columnNo)
                }
            } else {
                columnNo++
            }

            if (acceptable.isNotEmpty()) {
                // Compute all DFAs currently in an accept state that could accept another character, if one exists.
                // This will give us the list of DFAs that can still maximal munch.
                val maximal = acceptable.filter {
                    i + 1 < text.toCharArray().size && it.peek(text.toCharArray()[i + 1])
                }

                // If no DFAs can still maximal munch, then select the DFA with the highest priority.
                // Otherwise, defer this decision to the next iteration
                val machine = if (maximal.isEmpty()) {
                    acceptable[0]
                } else {
                    null
                }

                machine?.let {
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
