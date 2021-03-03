package com.mktiti.fsearch.client.cli.tui

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

interface KotlinCompleter : Completer {

    companion object {
        val NOP = object : KotlinCompleter {
            override fun complete(parts: List<String>, current: String) = emptyList<String>()
        }
    }

    class StringCompleter(private val values: List<String>) : KotlinCompleter {
        override fun complete(parts: List<String>, current: String): List<String> = values
    }

    class ConcatCompleter(
            private val primary: KotlinCompleter,
            private val secondary: KotlinCompleter
    ) : KotlinCompleter {
        override fun complete(parts: List<String>, current: String): List<String>
            = primary.complete(parts, current) + secondary.complete(parts, current)
    }

    fun complete(parts: List<String>, current: String): List<String>

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        candidates += complete(line.words().take(line.wordIndex()), line.word()).map {
            Candidate(it)
        }
    }

}

class CommandQueryCompleter(
        private val queryCompleter: KotlinCompleter,
        private val commandCompleter: KotlinCompleter
) : KotlinCompleter {

    companion object {
        private const val commandPrefix = ":"
    }

    private fun completeCommand(parts: List<String>, current: String): List<String> {
        val (cleanedParts, cleanedCurrent) = if (parts.isEmpty()) {
            emptyList<String>() to current.removePrefix(commandPrefix)
        } else {
            val cleanedParts = parts.mapIndexed { i, p ->
                if (i == 0) p.removePrefix(commandPrefix) else p
            }
            cleanedParts to current
        }

        val options = commandCompleter.complete(cleanedParts, cleanedCurrent)
        return if (parts.isEmpty()) {
            options.map { "$commandPrefix$it" }
        } else {
            options
        }
    }

    override fun complete(parts: List<String>, current: String): List<String> {
        if (parts.isEmpty() && current.isBlank()) {
            return completeCommand(parts, current)
        }

        val root = parts.firstOrNull() ?: current
        return if (root.startsWith(commandPrefix)) {
            completeCommand(parts, current)
        } else {
            queryCompleter.complete(parts, current)
        }
    }

}