package com.mktiti.fsearch.client.cli

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

    fun complete(parts: List<String>, current: String): List<String>

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        candidates += complete(line.words().drop(line.wordIndex() + 1), line.word()).map {
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

    override fun complete(parts: List<String>, current: String): List<String> {
        val root = parts.firstOrNull() ?: current
        return if (root.startsWith(commandPrefix)) {
            val options = commandCompleter.complete(parts, current.removePrefix(commandPrefix))
            if (parts.isEmpty()) {
                options.map { "$commandPrefix$it" }
            } else {
                options
            }
        } else {
            queryCompleter.complete(parts, current)
        }
    }

}