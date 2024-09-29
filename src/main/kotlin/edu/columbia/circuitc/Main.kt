package edu.columbia.circuitc

import edu.columbia.circuitc.lexer.Lexer
import java.io.File

fun main(args: Array<String>) {
    val content = readInputFile(args)

    val lexer = Lexer()
    val tokens = lexer.tokenize(content)

    println(tokens)
}

private fun readInputFile(args: Array<String>): String {
    val fileName = getInputFile(args)
    return File(fileName).readText()
}

private fun getInputFile(args: Array<String>): String {
    if (args.size != 1) {
        error("usage: circuitcc <file>")
    }

    val fileName = args[0]

    if (!fileName.endsWith(".circuit")) {
        error("$fileName does not end with .circuit")
    }

    return fileName
}
