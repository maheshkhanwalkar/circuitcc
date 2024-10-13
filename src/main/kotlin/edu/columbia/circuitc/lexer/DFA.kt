package edu.columbia.circuitc.lexer

/**
 * Build a DFA from the given directed graph.
 */
class DFA(private val name: String, private val transitionMap: Map<Int, Map<Char, Int>>,
          private val startState: Int, private val acceptStates: Set<Int>, private val acceptor: TokenAcceptor) {

    private var state = startState
    private var prevState = startState
    private val seenText = StringBuilder()

    /**
     * Consume a character, performing a state transition.
     */
    fun consume(c: Char): Boolean {
        val nextState = transitionMap[state]?.get(c)

        // Shorthand, just reset to original state if we encounter a char that doesn't have a transition
        // This allows us to avoid needing to encode every char in the transition table.
        if (nextState == null) {
            this.prevState = this.state
            reset()
            return false
        }

        this.seenText.append(c)
        this.prevState = this.state
        this.state = nextState
        return true
    }

    /**
     * Check whether consuming another character would keep the DFA in an accept
     * state -- without actually updating any internal DFA state.
     */
    fun peek(c: Char): Boolean {
        if (!isAccept()) {
            return false
        }

        val nextState = transitionMap[state]?.get(c)
        return nextState in acceptStates
    }

    /**
     * Is the DFA in an accept state or not?
     */
    fun isAccept(): Boolean {
        return state in acceptStates
    }

    /**
     * If the DFA is in an accept state, generate a token.
     */
    fun accept(start: TokenPos, end: TokenPos): Token {
        if (!isAccept()) {
            throw IllegalStateException("State $state is not an accept state")
        }

        val token = acceptor.accept(seenText.toString(), start, end)
        reset()

        return token
    }

    /**
     * Reset the DFA (so it can be used again)
     */
    fun reset() {
        seenText.clear()
        state = startState
    }

    /**
     * Rewind the DFA by a single character. Only works one time -- you cannot rewind back through
     * the entire state transition history.
     */
    fun rewindOnce() {
        this.state = this.prevState
    }
}
