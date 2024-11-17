package edu.columbia.circuitc.sym

/**
 * Symbol table implementation with scoping.
 *
 * This is an extension of a normal symbol table which maps keys to values by keeping track of
 * the scope that keys belong to. Keys in outer scopes are still visible in inner scopes, but not
 * the other way round.
 *
 * **Limitation:** this implementation does not permit "shadowing" or redefinition of keys either in the
 * same scope or across scopes, as that makes bookkeeping and leaving a scope more complicated -- which
 * isn't needed for our use-case.
 */
class SymbolTable<T> {
    private val symbols = mutableMapOf<String, T>()
    private val scopeStack = ArrayDeque<MutableList<String>>(listOf(mutableListOf()))

    fun enterScope() {
        scopeStack.addFirst(mutableListOf())
    }

    fun leaveScope() {
        if (scopeStack.size == 1) {
            throw IllegalStateException("cannot leave default scope")
        }

        scopeStack.removeFirst().forEach {
            symbols.remove(it)
        }
    }

    fun put(symbol: String, value: T) {
        if (symbols.contains(symbol)) {
            throw IllegalArgumentException("cannot redefine symbol $symbol")
        }

        symbols[symbol] = value
        scopeStack.first().add(symbol)
    }

    fun get(symbol: String): T? {
        return symbols[symbol]
    }
}
