package edu.columbia.circuitc.lexer

/**
 * Type of lexical token.
 */
enum class TokenType {
    AND, OR, NOT, XOR,
    IN, OUT, BITS,
    LEFT_ANGLE, RIGHT_ANGLE, EQUALS, LEFT_BRACE, RIGHT_BRACE, LEFT_PAREN, RIGHT_PAREN, SEMICOLON,
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
