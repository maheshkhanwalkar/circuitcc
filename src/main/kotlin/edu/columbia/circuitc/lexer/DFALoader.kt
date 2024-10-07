package edu.columbia.circuitc.lexer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

object DFALoader {
    private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val dfaMap: Map<String, DFAJson> = loadDFAs()

    private fun loadDFAs(): Map<String, DFAJson> {
        val stream = this::class.java.getResourceAsStream("/lexer/tokens.json")
        val graphMapRaw = mapper.readValue(stream, Map::class.java)

        return graphMapRaw.map {
            it.key as String to mapper.convertValue(it.value, DFAJson::class.java)
        }.toMap()
    }

    /**
     * Expand any string shorthands (e.g. "0-9" -> 0, 1, 2, 3, 4, 5, 6, 7, 8, 9) and construct
     * a graph with the expanded character edges.
     */
    private fun expandShortHand(graph: Map<Int, Map<String, Int>>): Map<Int, Map<Char, Int>> {
        return graph.map {
            val transformed = it.value.flatMap { transition ->
                if (transition.key.length > 1) {
                    val keys = if (transition.key.contains('-')) {
                        (transition.key[0]..transition.key[2]).toSet()
                    } else {
                        // Handle explicit single-char alternation
                        val options = transition.key.split("|")
                        options.map { item ->
                            if (item.length == 1) {
                                item[0]
                            } else {
                                when (item[1]) {
                                    't' -> '\t'
                                    'n' -> '\n'
                                    'r' -> '\r'
                                    else -> {
                                        throw IllegalArgumentException("Illegal character $item")
                                    }
                                }
                            }
                        }.toSet()
                    }

                    keys.map { key -> key to transition.value }
                } else {
                    listOf(transition.key[0] to transition.value)
                }
            }.toMap()

            it.key to transformed
        }.toMap()
    }

    /**
     * Get the DFA by name with the provided acceptor function.
     */
    fun get(name: String, acceptor: TokenAcceptor): DFA {
        val rawDFA = dfaMap[name]!!
        val expanded = expandShortHand(rawDFA.graph)
        return DFA(expanded, rawDFA.start, rawDFA.accept, acceptor)
    }
}

/**
 * JSON data object representing a DFA.
 */
private data class DFAJson(var start: Int, var accept: Set<Int>, var graph: Map<Int, Map<String, Int>>)
