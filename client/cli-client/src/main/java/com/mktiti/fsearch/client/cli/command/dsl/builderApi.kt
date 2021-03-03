package com.mktiti.fsearch.client.cli.command.dsl

import com.mktiti.fsearch.client.cli.context.Context
import com.mktiti.fsearch.client.cli.job.BackgroundJob
import com.mktiti.fsearch.client.cli.job.BackgroundJobContext
import com.mktiti.fsearch.client.cli.tui.KotlinCompleter

typealias CommandSetup = CommandBuilder.() -> Unit

typealias CommandHandle<R> = BackgroundJobContext.(List<String>) -> R
typealias VoidCommandHandle = CommandHandle<Unit>
typealias TransformCommandHandle = CommandHandle<Context>

typealias CommandHelper = (args: List<String>) -> String
typealias CommandCompleter = (parts: List<String>, current: String) -> List<String>

interface CommandBuilder {

    companion object {
        fun Int?.asRange(): IntRange = if (this == null) IntRange.EMPTY else (this..this)
    }

    fun subCommand(name: String, paramCount: Int? = null, setup: CommandSetup)

    fun help(helpCreator: CommandHelper)

    fun handle(paramCount: Int? = null, handler: VoidCommandHandle) = handleRange(paramCount.asRange(), handler)

    fun handleRange(paramRange: IntRange = IntRange.EMPTY, handler: VoidCommandHandle) = handleTransformRange(paramRange, handler.asTransform())

    fun handleTransform(paramCount: Int? = null, handler: TransformCommandHandle) = handleTransformRange(paramCount.asRange(), handler)

    fun handleTransformRange(paramRange: IntRange = IntRange.EMPTY, handler: TransformCommandHandle)

    fun complete(appendSubCommands: Boolean = true, completer: CommandCompleter)

}

interface CommandContext {

    val completer: KotlinCompleter

    fun handle(args: List<String>): BackgroundJob

}

interface RootBuilder {
    fun command(name: String, paramCount: Int? = null, setup: CommandSetup)
}