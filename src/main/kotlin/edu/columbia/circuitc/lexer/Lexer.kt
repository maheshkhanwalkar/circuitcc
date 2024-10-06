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

        // Symbols
        DFALoader.get("<") { text, start, end -> Token(TokenType.LEFT_ANGLE, text, start, end) },
        DFALoader.get(">") { text, start, end -> Token(TokenType.RIGHT_ANGLE, text, start, end) },
        DFALoader.get("=") { text, start, end -> Token(TokenType.EQUALS, text, start, end) },
        DFALoader.get("{") { text, start, end -> Token(TokenType.LEFT_BRACE, text, start, end) },
        DFALoader.get("}") { text, start, end -> Token(TokenType.RIGHT_BRACE, text, start, end) },
        DFALoader.get("(") { text, start, end -> Token(TokenType.LEFT_PAREN, text, start, end) },
        DFALoader.get(")") { text, start, end -> Token(TokenType.RIGHT_PAREN, text, start, end) },
        DFALoader.get(";") { text, start, end -> Token(TokenType.SEMICOLON, text, start, end) },

        // Identifiers/Numbers
        DFALoader.get("id")      { text, start, end -> Token(TokenType.IDENTIFIER, text, start, end) },
        DFALoader.get("num") { text, start, end -> Token(TokenType.INT, text, start, end) }
    )

    fun tokenize(text: String): List<Token> {
        var lineNo = 1
        var columnNo = 1

        val tokens = mutableListOf<Token>()
        var start = TokenPos(lineNo, columnNo)

        for (c in text.toCharArray()) {
            var found = false

            for (machine in stateMachines) {
                if (!machine.isAccept() || machine.peek(c)) {
                    machine.consume(c)
                    continue
                }

                // State machine is in an accept state and cannot maximal munch
                val token = machine.accept(start, TokenPos(lineNo, columnNo))
                tokens.add(token)
                found = true
                break
            }

            if (found) {
                // We still need to consume the current character
                stateMachines.forEach {
                    it.reset()
                    it.consume(c)
                }

                start = TokenPos(lineNo, columnNo)
            }

            if (c == '\n') {
                lineNo++
                columnNo = 1
            } else {
                columnNo++
            }
        }

        // Do one final loop through the state machines to consume the last token
        for (machine in stateMachines) {
            if (machine.isAccept()) {
                val token = machine.accept(start, TokenPos(lineNo, columnNo))
                tokens.add(token)
                break
            }
        }

        stateMachines.forEach { it.reset() }
        return tokens
    }
}
