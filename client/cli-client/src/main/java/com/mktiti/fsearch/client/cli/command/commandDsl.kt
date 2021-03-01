package com.mktiti.fsearch.client.cli.command

import com.mktiti.fsearch.client.cli.*
import com.mktiti.fsearch.util.safeCutHead

typealias CommandSetup = CommandBuilder.() -> Unit

typealias CommandHandle = BackgroundJobContext.(List<String>) -> Unit

interface CommandBuilder {

    var handler: CommandHandle

    var help: (List<String>) -> String

    var complete: KotlinCompleter

    fun subCommand(name: String, paramCount: Int? = null, setup: CommandSetup)

}

private fun helpPrintJob(args: List<String>, helper: (List<String>) -> String): BackgroundJob = printJob(helper(args))

private fun buildContext(
        subCommands: Map<String, CommandContext>,
        selfComplete: KotlinCompleter,
        selfHelper: (List<String>) -> String,
        selfHandle: CommandHandle
): CommandContext {
    fun selfHandleJob(args: List<String>): BackgroundJob = {
        selfHandle(args)
    }

    return object : CommandContext {
        override val completer = object : KotlinCompleter {
            override fun complete(parts: List<String>, current: String): List<String> {
                return when (val split = parts.safeCutHead()) {
                    null -> selfComplete.complete(parts, current) + subCommands.keys.toList()
                    else -> when (val command = subCommands[split.first]) {
                        null -> selfComplete.complete(parts, current)
                        else -> command.completer.complete(split.second, current)
                    }
                }
            }
        }

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
}

private class DefaultCommandBuilder : CommandBuilder {

    private val commandMap = mutableMapOf<String, CommandContext>()

    fun build(): CommandContext {
        return buildContext(
                subCommands = commandMap,
                selfHandle = handler,
                selfComplete = complete,
                selfHelper = help
        )
    }

    override var handler: CommandHandle = { /* Nop */ }

    override var help: (List<String>) -> String = { _ ->
        "" // NOP helper
    }

    override var complete: KotlinCompleter = KotlinCompleter.NOP

    override fun subCommand(name: String, paramCount: Int?, setup: CommandSetup) {
        commandMap[name] = DefaultCommandBuilder().let { subContext ->
            subContext.setup()
            subContext.build()
        }
    }

}

interface CommandContext {

    val completer: KotlinCompleter

    fun handle(args: List<String>): BackgroundJob

}

interface RootBuilder {
    var help: () -> String

    fun command(name: String, paramCount: Int? = null, setup: CommandSetup)
}

private class DefaultRootBuilder : RootBuilder {

    private val commandMap = mutableMapOf<String, CommandContext>()

    override var help = { "" /* NOP helper */ }

    fun build(): CommandContext {
        return buildContext(
                subCommands = commandMap,
                selfHandle = { NopBackgroundJob },
                selfComplete = KotlinCompleter.StringCompleter(commandMap.keys.toList()),
                selfHelper = { help() }
        )
    }

    override fun command(name: String, paramCount: Int?, setup: CommandSetup) {
        commandMap[name] = DefaultCommandBuilder().let { subContext ->
            subContext.setup()
            subContext.build()
        }
    }

}

fun createCommands(creator: RootBuilder.() -> Unit): CommandContext {
    return DefaultRootBuilder().let { rootBuilder ->
        rootBuilder.creator()
        rootBuilder.build()
    }
}
