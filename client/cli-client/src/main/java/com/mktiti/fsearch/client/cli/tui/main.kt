package com.mktiti.fsearch.client.cli.tui

import com.mktiti.fsearch.client.cli.ProjectInfo
import com.mktiti.fsearch.client.cli.command.CommandStore
import com.mktiti.fsearch.client.cli.context.Context
import com.mktiti.fsearch.client.cli.context.ContextManager
import com.mktiti.fsearch.client.cli.job.DefaultJobHandler
import com.mktiti.fsearch.client.cli.job.PrintWriterJobPrinter
import com.mktiti.fsearch.client.cli.search.SearchHandler
import com.mktiti.fsearch.client.cli.util.runHealthCheck
import com.mktiti.fsearch.client.rest.ClientFactory
import com.mktiti.fsearch.client.rest.ClientFactory.Config.RestConfig
import com.mktiti.fsearch.client.rest.nop.NopService
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.Completer
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

fun main(args: Array<String>) {

    val terminal: Terminal = with(TerminalBuilder.builder()) {
        system(true)

        build().apply {
            if (width == 0 || height == 0) {
                size = Size(120, 40)
            }
        }
    }

    val completer: Completer = CommandQueryCompleter(
        queryCompleter = KotlinCompleter.NOP,
        commandCompleter = CommandStore.completer
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
    printer.println(ProjectInfo.versionedName)

    val initialService = when (val backend = args.firstOrNull()) {
        null -> {
            printer.println("WARNING: Service path not set (program argument), use ':service set' to define")
            NopService
        }
        else -> {
            ClientFactory.create(RestConfig(backend)).apply {
                runHealthCheck(this, PrintWriterJobPrinter(printer))
            }
        }
    }

    val searchHandler = SearchHandler()
    val contextManager = ContextManager(Context(initialService, emptySet(), emptyList()))

    DefaultJobHandler(printer, contextManager).use { jobHandler ->
        terminal.handle(Terminal.Signal.INT) {
            if (jobHandler.cancel()) {
                printer.println("Interrupted")
            }
        }

        fun runCommand(line: String): Boolean {
            val job = if (line.startsWith(":")) {
                CommandStore.handle(line.removePrefix(":").split("\\s+".toRegex()))
            } else {
                searchHandler.searchJob(line)
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