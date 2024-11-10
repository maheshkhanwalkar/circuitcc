package edu.columbia.circuitc.printer

import edu.columbia.circuitc.lexer.TokenPos

class PrettyPrinter(private val fileName: String, fileContent: String) {
    private val lines: List<String> = fileContent.lines()

    fun printMessage(msg: String, start: TokenPos, end: TokenPos) {
        print("$fileName:${start.row}:${start.col} ")
        printColor("error: ", Color.RED, false)
        println(msg)

        for (rowPos in (start.row-1)..<end.row) {
            val line = lines[rowPos]
            println(line)

            if (rowPos == start.row - 1) {
                print(" ".repeat(start.col - 1))
                printColor("^", Color.GREEN, false)

                if (start.row == end.row) {
                    val underline = "~".repeat(end.col - start.col - 1)
                    printColor(underline, Color.GREEN, true)
                }
            }
        }
    }

    private fun printColor(msg: String, color: Color, newline: Boolean) {
        val colorCode = color.escapeCode
        val reset = "\u001b[0m"

        val fullMessage = colorCode + msg + reset

        if (newline) {
            println(fullMessage)
        } else {
            print(fullMessage)
        }
    }
}

private enum class Color(val escapeCode: String) {
    RED("\u001b[31m"), GREEN("\u001b[32m")
}
