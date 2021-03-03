package com.mktiti.fsearch.client.cli.tui

import com.mktiti.fsearch.client.cli.*
import com.mktiti.fsearch.client.cli.command.CommandStore
import com.mktiti.fsearch.client.cli.context.Context
import com.mktiti.fsearch.client.cli.context.ContextImports
import com.mktiti.fsearch.client.cli.context.ContextManager
import com.mktiti.fsearch.client.cli.job.DefaultJobHandler
import com.mktiti.fsearch.client.cli.search.SearchHandler
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.fuel.FuelService
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.Completer
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.util.concurrent.atomic.AtomicReference

fun main(args: Array<String>) {

    val basePath = args.firstOrNull() ?: error("API path (first param) missing!")
    //val client = RestClient.forPath(basePath)
    val client = FuelService(basePath)

    /*val parser: Parser = DefaultParser().apply {
        eofOnUnclosedBracket(Bracket.ANGLE, Bracket.CURLY, Bracket.ROUND, Bracket.SQUARE)
    }
     */

    val terminal: Terminal = with(TerminalBuilder.builder()) {
        system(true)

        build().apply {
            if (width == 0 || height == 0) {
                size = Size(120, 40)
            }
        }
    }

    val commandStore = CommandStore(client)
    val completer: Completer = CommandQueryCompleter(
        queryCompleter = KotlinCompleter.NOP,
        commandCompleter = commandStore.completer
    )

    val lineReader = with(LineReaderBuilder.builder()) {
        // parser(parser)
        appName("JvmSearch TUI Client")
        terminal(terminal)
        completer(completer)
        build()
    }

    SystemRegistryImpl(null, terminal, { null }, null)

    val printer = terminal.writer()

    printer.println("JvmSearch CLI Client v${ProjectInfo.version}")
    printer.print("Running API health check... ")

    when (val checkRes = client.searchApi.healthCheck()) {
        is ApiCallResult.Success -> printer.println(checkRes.result.message)
        is ApiCallResult.Exception -> printer.println("[${checkRes.code}] - ${checkRes.message}")
    }

    val searchHandler = SearchHandler(client)

    val contextManager = ContextManager()

    DefaultJobHandler(printer, contextManager).use { jobHandler ->
        terminal.handle(Terminal.Signal.INT) {
            if (jobHandler.cancel()) {
                printer.println("Interrupted")
            }
        }

        val context = AtomicReference(
                Context(emptySet(), ContextImports.empty())
        )

        fun runCommand(line: String): Boolean {
            val job = if (line.startsWith(":")) {
                commandStore.handle(line.removePrefix(":").split("\\s+".toRegex()))
            } else {
                searchHandler.searchJob(context.get(), line)
            }
            return jobHandler.runJob(job)
        }

        while (true) {
            try {
                val line = lineReader.readLine("λ>")?.trim() ?: break
                if (line.isNotBlank()) {
                    if (runCommand(line)) {
                        break
                    }
                }
            } catch (interrupt: UserInterruptException) {
                if (!jobHandler.cancel()) {
                    printer.println("No active job to kill, use :quit to exit JvmSearch")
                }
            } catch (fileEnd: EndOfFileException) {
                fileEnd.partialLine?.let(::runCommand)
                break
            }
        }

        printer.println("Exiting")
    }

}