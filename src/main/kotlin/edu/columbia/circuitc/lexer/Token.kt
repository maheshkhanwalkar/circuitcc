package edu.columbia.circuitc.lexer

/**
 * Type of lexical token.
 */
enum class TokenType {
    AND, OR, NOT, XOR,
    IN, OUT, BITS,
    IDENTIFIER, INT
}

/**
 * Token position in original source text.
 */
data class TokenPos(val row: Int, val col: Int)

/**
 * Lexical token.
 */
data class Token(val type: TokenType, val text: String, val start: TokenPos, val end: TokenPos)

/**
 * Build a token from the given text.
 */
fun interface TokenAcceptor {
    fun accept(text: String, start: TokenPos, end: TokenPos): Token
}
