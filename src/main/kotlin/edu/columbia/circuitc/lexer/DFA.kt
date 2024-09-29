package edu.columbia.circuitc.lexer

/**
 * Build a DFA from the given regex with a token acceptor.
 *
 * Caveat: this class does not support all kinds of regular expressions. It does not support alternation
 * and most regex syntax sugar.
 */
class DFA(regex: String, private val acceptor: TokenAcceptor) {
    private val transitionMap: Map<Int, Map<Char, Int>>
    private val acceptStates: Set<Int>
    private val seenText = StringBuilder()

    private var state = 0

    init {
        val state = buildTransitionMap(regex)
        this.transitionMap = state.first
        this.acceptStates = state.second
    }

    /**
     * Consume a character, performing a state transition.
     */
    fun consume(c: Char) {
        val nextState = transitionMap[state]?.get(c)

        // Shorthand, just reset to original state if we encounter a char that doesn't have a transition
        // This allows us to avoid needing to encode every char in the transition table.
        if (nextState == null) {
            reset()
            return
        }

        this.seenText.append(c)
        this.state = nextState
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
        state = 0
    }

    private fun buildTransitionMap(regex: String): Pair<Map<Int, Map<Char, Int>>, Set<Int>> {
        var currState = 0
        val map = mutableMapOf<Int, Map<Char, Int>>()

        for (c in regex) {
            // TODO handle star and alternation syntax
            val nextState = currState + 1
            map[currState] = mapOf(Pair(c, nextState))
            currState = nextState
        }

        return Pair(map, setOf(currState))
    }
}
