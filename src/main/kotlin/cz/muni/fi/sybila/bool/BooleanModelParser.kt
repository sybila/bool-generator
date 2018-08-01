package cz.muni.fi.sybila.bool

import java.io.File

class BooleanModelParser {

    fun readFile(file: File): BooleanModel = readString(file.readText())

    fun readString(string: String): BooleanModel {
        val assignments: List<Pair<Id, String>> = string.lines().mapIndexed { lineIndex, line ->
            val split = line.split("<-")
            if (split.size != 2) error("Invalid line $lineIndex: Cannot determine left/right sides of assignment.")
            val assignId = parseId(split[0])
            assignId to split[1].trim()
        }

        val levelDeclarations = assignments.map { it.first }

        val variableLevels = levelDeclarations.groupBy({ it.name }, { it.value })

        val variables = variableLevels.keys.sorted()
        val variableToIndex = variables.mapIndexed { i, v -> v to i }.toMap()

        // Check if all levels are defined
        for ((v, levels) in variableLevels) {
            val maxLevel = levels.max() ?: 0
            for (l in 1 until maxLevel) {
                if (l !in levels) error("Missing assignment for $v:$l.")
            }
        }

        // Check if some levels are not duplicate
        val totalCount = variableLevels.map { it.value.size }.sum()
        if (totalCount != assignments.size) error("Some assignments are duplicated!")

        val variableToMaxLevel: Map<String, Int> = variables.map { v -> v to variableLevels[v]!!.max()!! }.toMap()

        val functionDeclarations = assignments.map { (id, string) ->
            val tokens = string.tokenize()
            val (function, index) = readFunction(variableToIndex, tokens, 0)
            if (index < tokens.size) error("Unexpected ${tokens.subList(index, tokens.size)}")
            id to function
        }.toMap()

        return BooleanModel(
                variables.map { v ->
                    val maxLevel = variableToMaxLevel[v]!!
                    BooleanModel.Variable(name = v, maxLevel = maxLevel, levelFunctions =
                        (1..maxLevel).map { l -> functionDeclarations[Id(v, l)]!! }
                    )
                }
        )
    }

    private fun parseId(string: String): Id {
        val split = string.trim().split(':')
        return when (split.size) {
            1 -> Id(split[0], 1)
            2 -> Id(split[0], split[1].toInt())
            else -> error("Invalid id $string.")
        }
    }

    private data class Id(val name: String, val value: Int)

    private fun String.tokenize(): List<Token> {
        val result = ArrayList<Token>()
        var activeIndex = 0
        while (activeIndex < length) {
            val activeChar = this[activeIndex]
            when {
                activeChar.isWhitespace() -> {
                    activeIndex += 1
                }
                activeChar == '&' -> {
                    result.add(Token.And)
                    activeIndex += 1
                }
                activeChar == '|' -> {
                    result.add(Token.Or)
                    activeIndex += 1
                }
                activeChar == '!' -> {
                    result.add(Token.Not)
                    activeIndex += 1
                }
                activeChar == '(' -> {
                    result.add(Token.ParOpen)
                    activeIndex += 1
                }
                activeChar == ')' -> {
                    result.add(Token.ParClose)
                    activeIndex += 1
                }
                activeChar == ':' -> {
                    result.add(Token.Delimiter)
                    activeIndex += 1
                }
                activeChar.isDigit() -> {
                    val startIndex = activeIndex
                    while (activeIndex < length && this[activeIndex].isDigit()) activeIndex += 1
                    result.add(Token.Number(substring(startIndex, activeIndex).toInt()))
                }
                activeChar.isLetter() -> {
                    val startIndex = activeIndex
                    while (activeIndex < length && this[activeIndex].isLetterOrDigit()) activeIndex += 1
                    result.add(Token.Name(substring(startIndex, activeIndex)))
                }
                else -> {
                    error("Unexpected char $activeChar at position $activeIndex")
                }
            }
        }
        return result
    }

    private fun readFunction(variableToIndex: Map<String, Int>, tokens: List<Token>, startAt: Int): Pair<BooleanFunction, Int>
            = readOr(variableToIndex, tokens, startAt)

    private fun readOr(variableToIndex: Map<String, Int>, tokens: List<Token>, startAt: Int): Pair<BooleanFunction, Int> {
        val (left, afterLeft) = readAnd(variableToIndex, tokens, startAt)
        return if (afterLeft >= tokens.size || tokens[afterLeft] !== Token.Or) left to afterLeft else {
            val (right, afterRight) = readOr(variableToIndex, tokens, afterLeft + 1)
            (left or right) to afterRight
        }
    }

    private fun readAnd(variableToIndex: Map<String, Int>, tokens: List<Token>, startAt: Int): Pair<BooleanFunction, Int> {
        val (left, afterLeft) = readNot(variableToIndex, tokens, startAt)
        return if (afterLeft >= tokens.size || tokens[afterLeft] !== Token.And) left to afterLeft else {
            val (right, afterRight) = readOr(variableToIndex, tokens, afterLeft + 1)
            (left and right) to afterRight
        }
    }

    private fun readNot(variableToIndex: Map<String, Int>, tokens: List<Token>, startAt: Int): Pair<BooleanFunction, Int> {
        return if (startAt >= tokens.size || tokens[startAt] !== Token.Not) {
            readLiteral(variableToIndex, tokens, startAt)
        } else {
            val (f, continueAt) = readNot(variableToIndex, tokens, startAt + 1)
            !f to continueAt
        }
    }

    private fun readLiteral(variableToIndex: Map<String, Int>, tokens: List<Token>, startAt: Int): Pair<BooleanFunction, Int> {
        if (startAt >= tokens.size || (tokens[startAt] !is Token.Name && tokens[startAt] !== Token.ParOpen)) {
            error("Expected literal")
        }
        return if (tokens[startAt] === Token.ParOpen) readParenthesis(variableToIndex, tokens, startAt) else {
            readAtLeast(variableToIndex, tokens, startAt)
        }
    }

    private fun readParenthesis(variableToIndex: Map<String, Int>, tokens: List<Token>, openIndex: Int): Pair<BooleanFunction, Int> {
        if (openIndex >= tokens.size || tokens[openIndex] !== Token.ParOpen) error("Expected opening parenthesis")
        val (function, continueAt) = readFunction(variableToIndex, tokens, openIndex + 1)
        if (continueAt >= tokens.size || tokens[continueAt] !== Token.ParClose) error("Expected closing parenthesis")
        return function to (continueAt + 1)
    }

    private fun readAtLeast(variableToIndex: Map<String, Int>, tokens: List<Token>, nameIndex: Int): Pair<BooleanFunction, Int> {
        val delimiterIndex = nameIndex + 1
        val numberIndex = nameIndex + 2
        if (nameIndex >= tokens.size) error("Expected name, found EOL")
        val name = tokens[nameIndex]
        if (name !is Token.Name) error("Expecting name, found $name")
        val index = variableToIndex[name.value] ?: error("Unknown variable $name")
        return if (delimiterIndex >= tokens.size || tokens[delimiterIndex] !== Token.Delimiter) {
            atLeast(index, 1) to (nameIndex + 1)
        } else {
            if (numberIndex >= tokens.size) error("Expecting number after ':'")
            val number = tokens[numberIndex]
            if (number !is Token.Number) error("Expecting number, found $number")
            atLeast(index, number.value) to (numberIndex + 1)
        }
    }

    private sealed class Token {
        object And : Token()        // &
        object Or : Token()         // |
        object Not : Token()        // !
        object Delimiter : Token()  // :
        object ParOpen : Token()    // (
        object ParClose : Token()   // )
        data class Name(val value: String) : Token()
        data class Number(val value: Int) : Token()
    }

}