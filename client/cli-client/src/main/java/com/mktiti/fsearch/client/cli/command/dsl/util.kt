package com.mktiti.fsearch.client.cli.command.dsl

import com.mktiti.fsearch.client.cli.job.BackgroundJob
import com.mktiti.fsearch.client.cli.job.printJob
import com.mktiti.fsearch.client.cli.tui.KotlinCompleter

val nopTransformCommandHandle: TransformCommandHandle = { this.context }

fun CommandCompleter.wrap(): KotlinCompleter = object : KotlinCompleter {
    override fun complete(parts: List<String>, current: String) = invoke(parts, current)
}

fun VoidCommandHandle.asTransform(): TransformCommandHandle = { args ->
    this@asTransform(this, args)
    this.context
}

fun helpPrintJob(args: List<String>, helper: (List<String>) -> String): BackgroundJob = printJob(helper(args))