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

private class DefaultCommandBuilder : CommandBuilder {

    private val commandMap = mutableMapOf<String, CommandContext>()

    private var handler: TransformCommandHandle = nopTransformCommandHandle

    private var helper: CommandHelper? = null

    private var completeWithSubs: Boolean = true
    private var completer: CommandCompleter? = null

    fun build(): CommandContext {
        val commandNames = (commandMap.keys + "help").sorted()
        val finalCompleter: KotlinCompleter = if (completeWithSubs) {
            val commands = SubCommandCompleter(commandNames)
            completer?.let {
                KotlinCompleter.ConcatCompleter(it.wrap(), commands)
            } ?: commands
        } else {
            completer?.wrap() ?: KotlinCompleter.NOP
        }

        val commandHelp = if (commandNames.isEmpty()) {
            ""
        } else {
            buildString {
                appendLine("Available subcommands:")
                commandNames.forEach {
                    append("\t")
                    appendLine(it)
                }
            }
        }

        val finalHelper: CommandHelper = helper?.let { setHelper ->
            { args -> setHelper(args) + "\n\n" + commandHelp }
        } ?: { commandHelp }

        return DefaultCommandContext(
                subCommands = commandMap,
                selfHandle = handler,
                selfComplete = finalCompleter,
                selfHelper = finalHelper
        )
    }

    override fun subCommand(name: String, paramCount: Int?, setup: CommandSetup) {
        commandMap[name] = DefaultCommandBuilder().let { subContext ->
            subContext.setup()
            subContext.build()
        }
    }

    override fun handleTransform(handler: TransformCommandHandle) {
        this.handler = handler
    }

    override fun help(helpCreator: (args: List<String>) -> String) {
        this.helper = helpCreator
    }

    override fun complete(appendSubCommands: Boolean, completer: CommandCompleter) {
        this.completer = completer
    }

}

private class DefaultRootBuilder : RootBuilder {

    private val commandMap = mutableMapOf<String, CommandContext>()

    fun build(): CommandContext {
        return DefaultCommandContext(
                subCommands = commandMap,
                selfHandle = nopTransformCommandHandle,
                selfComplete = KotlinCompleter.StringCompleter(commandMap.keys.sorted()),
                selfHelper = { "" }
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
