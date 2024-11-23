package edu.columbia.circuitc

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import edu.columbia.circuitc.codegen.generateIR
import edu.columbia.circuitc.codegen.generateSIMCode
import edu.columbia.circuitc.codegen.sim.SimCircuit
import edu.columbia.circuitc.lexer.Lexer
import edu.columbia.circuitc.parser.Parser
import edu.columbia.circuitc.printer.PrettyPrinter
import edu.columbia.circuitc.printer.printAST
import java.io.File

fun main(args: Array<String>) {
    val content = readInputFile(args)
    val printer = PrettyPrinter(args[0], content)

    val lexer = Lexer()
    val tokens = lexer.tokenize(content)

    val parser = Parser(printer)
    val ast = parser.parse(tokens)

    printAST(ast)

    val ir = generateIR(ast)
    val sim = generateSIMCode(ir)

    val outputFile = getOutputFileName(args)
    writeSIMOutput(sim, outputFile)
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

private fun getOutputFileName(args: Array<String>): String {
    val sourceFileName = args[0]
    val pos = sourceFileName.indexOf(".circuit")
    return sourceFileName.substring(0, pos) + ".sim"
}

private fun writeSIMOutput(sim: SimCircuit, outputFile: String) {
    val mapper = ObjectMapper()
    mapper.writer(DefaultPrettyPrinter()).writeValue(File(outputFile), sim)
}
