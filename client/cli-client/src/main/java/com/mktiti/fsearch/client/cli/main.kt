package com.mktiti.fsearch.client.cli

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.fuel.FuelService
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.*
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.DefaultParser.Bracket
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.util.concurrent.atomic.AtomicReference

fun main(args: Array<String>) {

    val basePath = args.firstOrNull() ?: error("API path (first param) missing!")
    //val client = RestClient.forPath(basePath)
    val client = FuelService(basePath)

    val parser: Parser = DefaultParser().apply {
        eofOnUnclosedBracket(Bracket.ANGLE, Bracket.CURLY, Bracket.ROUND, Bracket.SQUARE)
    }

    val terminal: Terminal = with(TerminalBuilder.builder()) {
        system(true)

        build().apply {
            if (width == 0 || height == 0) {
                size = Size(120, 40)
            }
        }
    }

    val completer: Completer = StringsCompleter(
            "info", "load", "load-code", "load-doc", "list-artifact", "list-fun", "list-type", "doc", "delete"
    )

    val lineReader = with(LineReaderBuilder.builder()) {
        appName("JvmSearch TUI Client")
        terminal(terminal)
        completer(completer)
        build()
    }

    SystemRegistryImpl(null, terminal, { null }, null)

    val printer = terminal.writer()

    printer.println("JvmSearch CLI Client v${ProjectInfo.version}")
    printer.println("Running API health check...")

    when (val checkRes = client.searchApi.healthCheck()) {
        is ApiCallResult.Success -> printer.println(checkRes.result.message)
        is ApiCallResult.Exception -> printer.println("[${checkRes.code}] - ${checkRes.message}")
    }

    val searchHandler = SearchHandler(client)

    val jobHandler: JobHandler = DefaultJobHandler(printer)
    terminal.handle(Terminal.Signal.QUIT) {
        jobHandler.cancel()
    }

    val context = AtomicReference(
            Context(emptySet(), ContextImports.empty())
    )

    fun runCommand(line: String): Boolean {
        return if (line.startsWith(":")) {
            if (line == ":quit") {
                printer.println("Quitting")
                true
            } else {
                false
            }
        } else {
            jobHandler.setJob(searchHandler.searchJob(context.get(), line))
            false
        }
    }

    while (true) {
        try {
            val line = lineReader.readLine(">")?.trim() ?: break
            if (runCommand(line)) {
                break
            }
        } catch (interrupt: UserInterruptException) {
            printer.println("Quiting")
            break
        } catch (fileEnd: EndOfFileException) {
            fileEnd.partialLine?.let(::runCommand)
            break
        }
    }

    println("Exited")

}