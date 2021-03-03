package com.mktiti.fsearch.client.cli.command.dsl

import com.mktiti.fsearch.client.cli.job.BackgroundJob
import com.mktiti.fsearch.client.cli.job.printJob
import com.mktiti.fsearch.client.cli.tui.KotlinCompleter
import com.mktiti.fsearch.util.safeCutHead

private class CommandContextCompleter(
        private val subCommands: Map<String, CommandContext>,
        private val selfComplete: KotlinCompleter
) : KotlinCompleter {
    private fun selfComplete(parts: List<String>, current: String): List<String> {
        return selfComplete.complete(parts, current)
    }

    override fun complete(parts: List<String>, current: String): List<String> {
        val (cmdString, cmdArgs) = parts.safeCutHead() ?: return selfComplete(parts, current)

        return when (val command = subCommands[cmdString]) {
            null -> selfComplete(parts, current)
            else -> command.completer.complete(cmdArgs, current)
        }
    }
}

class DefaultCommandContext(
        private val subCommands: Map<String, CommandContext>,
        selfComplete: KotlinCompleter,
        private val selfHelper: CommandHelper,
        private val selfHandlers: HandlerStore
) : CommandContext {

    private fun selfHandleJob(args: List<String>): BackgroundJob {
        val handler = selfHandlers[args.size]
        return if (handler == null) {
            printJob("Invalid parameter count (${args.size})")
        } else {
            { handler(args) }
        }
    }

    override val completer: KotlinCompleter = CommandContextCompleter(subCommands, selfComplete)

    override fun handle(args: List<String>): BackgroundJob {
        val cut = args.safeCutHead() ?: return selfHandleJob(args)

        val (commandStr, subArgs) = cut
        val command: CommandContext? = subCommands[commandStr]

        return when {
            command != null -> command.handle(subArgs)
            commandStr == "help" -> helpPrintJob(subArgs, selfHelper)
            else -> selfHandleJob(args)
        }
    }
}