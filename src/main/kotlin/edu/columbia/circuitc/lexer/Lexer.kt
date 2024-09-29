package edu.columbia.circuitc.lexer

class Lexer {
    fun tokenize(text: String): List<Token> {
        var lineNo = 1
        var columnNo = 1

        val tokens = mutableListOf<Token>()

        for (c in text.toCharArray()) {
            TODO("consume the character")

            if (c == '\n') {
                lineNo++
                columnNo = 1
            } else {
                columnNo++
            }
        }

        return tokens
    }
}
