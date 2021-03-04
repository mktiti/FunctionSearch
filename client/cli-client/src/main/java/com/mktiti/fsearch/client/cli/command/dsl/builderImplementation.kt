package com.mktiti.fsearch.client.cli.command.dsl

import com.mktiti.fsearch.client.cli.tui.KotlinCompleter

private class SubCommandCompleter(
        private val commands: List<String>
) : KotlinCompleter {

    override fun complete(parts: List<String>, current: String): List<String> {
        return if (parts.isEmpty()) {
            commands
        } else {
            emptyList()
        }
    }

}

private fun completeCommandNames(commands: Set<String>) = (commands + "help").sorted()

private fun createCompleter(
        commandNames: List<String>,
        completer: CommandCompleter? = null,
        completeWithSubs: Boolean = true
): KotlinCompleter {
    return if (completeWithSubs) {
        val commands = SubCommandCompleter(commandNames)
        completer?.let {
            KotlinCompleter.ConcatCompleter(it.wrap(), commands)
        } ?: commands
    } else {
        completer?.wrap() ?: KotlinCompleter.NOP
    }
}

private fun createHelper(commands: List<String>, helper: CommandHelper?): CommandHelper {
    val commandHelp = if (commands.isEmpty()) {
        ""
    } else {
        buildString {
            appendLine("Available subcommands:")
            commands.forEach {
                append("\t")
                appendLine(it)
            }
        }
    }

    return helper?.let { setHelper ->
        { args -> setHelper(args) + "\n\n" + commandHelp }
    } ?: { commandHelp }
}

private class DefaultCommandBuilder : CommandBuilder {

    private val commandMap = mutableMapOf<String, CommandContext>()

    private val handlers: MutableHandlerStore = DefaultHandlerStore()

    private var helper: CommandHelper? = null

    private var completeWithSubs: Boolean = true
    private var completer: CommandCompleter? = null

    fun build(): CommandContext {
        val commandNames = completeCommandNames(commandMap.keys)

        return DefaultCommandContext(
                subCommands = commandMap,
                selfHandlers = handlers,
                selfComplete = createCompleter(commandNames, completer, completeWithSubs),
                selfHelper = createHelper(commandNames, helper)
        )
    }

    override fun subCommand(name: String, paramCount: Int?, setup: CommandSetup) {
        commandMap[name] = DefaultCommandBuilder().let { subContext ->
            subContext.setup()
            subContext.build()
        }
    }

    override fun handleTransformRange(paramRange: IntRange, handler: TransformCommandHandle) {
        this.handlers[paramRange] = handler
    }

    override fun help(helpCreator: (args: List<String>) -> String) {
        this.helper = helpCreator
    }

    override fun complete(appendSubCommands: Boolean, completer: CommandCompleter) {
        this.completer = completer
    }

}

private class DefaultRootBuilder : RootBuilder {

    companion object {
        private val defaultUnknownHandler: CommandHelper = { args -> "Unknown command ${args.firstOrNull() ?: ""}" }
    }

    private var helper: CommandHelper? = null
    private var unknownHandler: CommandHelper = defaultUnknownHandler
    private val commandMap = mutableMapOf<String, CommandContext>()

    fun build(): CommandContext {
        val handler: HandlerStore = DefaultHandlerStore().apply {
            default = unknownHandler.asHandle()
        }

        val commandNames = completeCommandNames(commandMap.keys)

        return DefaultCommandContext(
                subCommands = commandMap,
                selfHandlers = handler,
                selfComplete = createCompleter(commandNames),
                selfHelper = createHelper(commandNames, helper)
        )
    }

    override fun help(helpCreator: CommandHelper) {
        helper = helpCreator
    }

    override fun command(name: String, setup: CommandSetup) {
        commandMap[name] = DefaultCommandBuilder().let { subContext ->
            subContext.setup()
            subContext.build()
        }
    }

    override fun unknownCommand(helper: CommandHelper) {
        unknownHandler = helper
    }

}

fun createCommands(creator: RootBuilder.() -> Unit): CommandContext {
    return DefaultRootBuilder().let { rootBuilder ->
        rootBuilder.creator()
        rootBuilder.build()
    }
}
