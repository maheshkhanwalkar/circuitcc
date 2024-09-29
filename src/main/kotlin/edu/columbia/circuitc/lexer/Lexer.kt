package edu.columbia.circuitc.lexer

/**
 * Lexer with constituent DFA rules ranked in order of highest (first) to lowest (last) priority.
 */
class Lexer {
    private val stateMachines: List<DFA> = listOf(
        // Keywords
        DFA("and")  { text, start, end -> Token(TokenType.AND, text, start, end) },
        DFA("or")   { text, start, end -> Token(TokenType.OR, text, start, end)  },
        DFA("not")  { text, start, end -> Token(TokenType.NOT, text, start, end) },
        DFA("xor")  { text, start, end -> Token(TokenType.XOR, text, start, end) },
        DFA("in")   { text, start, end -> Token(TokenType.IN, text, start, end)  },
        DFA("out")  { text, start, end -> Token(TokenType.OUT, text, start, end) },
        DFA("bits") { text, start, end -> Token(TokenType.BITS, text, start, end) },

        // Symbols
        DFA("<") { text, start, end -> Token(TokenType.LEFT_ANGLE, text, start, end) },
        DFA(">") { text, start, end -> Token(TokenType.RIGHT_ANGLE, text, start, end) },
        DFA("=") { text, start, end -> Token(TokenType.EQUALS, text, start, end) },
        DFA("{") { text, start, end -> Token(TokenType.LEFT_PAREN, text, start, end) },
        DFA("}") { text, start, end -> Token(TokenType.RIGHT_PAREN, text, start, end) },

        // Identifiers/Numbers [TODO]
        // DFA("[a-z]+")      { text, start, end -> Token(TokenType.IDENTIFIER, text, start, end) },
        // DFA("[1-9][0-9]*") { text, start, end -> Token(TokenType.INT, text, start, end) }
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
