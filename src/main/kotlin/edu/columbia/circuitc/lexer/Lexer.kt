package edu.columbia.circuitc.lexer

class Lexer(private val stateMachines: List<DFA>) {
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
                stateMachines.forEach { it.reset() }
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
