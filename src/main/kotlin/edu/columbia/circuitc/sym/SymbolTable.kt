package edu.columbia.circuitc.sym

/**
 * Symbol table implementation with scoping.
 *
 * This is an extension of a normal symbol table which maps keys to values by keeping track of
 * the scope that keys belong to. Keys in outer scopes are still visible in inner scopes, but not
 * the other way round.
 *
 * This implementation also supports "shadowing" or key redefinition *across* scopes. When a key is
 * defined within a scope, it will shadow the existing key in outer scopes. Therefore, calls to get() will
 * return the inner-most-scope's value. When leaving a scope, then the current "shadowing" ends and future
 * calls to get() will return the previous value prior to entering that scope. Shadowing can be nested
 * which unwinds as we go back up to the default scope. One key restriction is that keys *cannot* be redefined
 * within the same scope ("overwrite").
 */
class SymbolTable<T> {
    private val symbols = mutableMapOf<String, MutableList<T>>()
    private val scopeStack = ArrayDeque<MutableSet<String>>(listOf(mutableSetOf()))

    fun enterScope() {
        scopeStack.addFirst(mutableSetOf())
    }

    fun leaveScope() {
        if (scopeStack.size == 1) {
            throw IllegalStateException("cannot leave default scope")
        }

        scopeStack.removeFirst().forEach {
            symbols[it]?.removeLast()
        }
    }

    fun put(symbol: String, value: T) {
        val currScope = scopeStack.first()

        if (symbol in symbols) {
            if (symbol in currScope) {
                throw IllegalArgumentException("cannot redefine symbol $symbol, as it already exists in current scope")
            }

            symbols[symbol]?.add(value)
        }

        symbols[symbol] = mutableListOf(value)
        currScope.add(symbol)
    }

    fun get(symbol: String): T? {
        return symbols[symbol]?.lastOrNull()
    }
}
