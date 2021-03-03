package com.mktiti.fsearch.client.cli.job

import com.mktiti.fsearch.client.cli.context.Context
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicBoolean

typealias BackgroundJob = BackgroundJobContext.() -> Context

fun voidBackgroundJob(voidJob: BackgroundJobContext.() -> Unit): BackgroundJob = {
    voidJob()
    this.context
}

fun printJob(message: String): BackgroundJob = voidBackgroundJob {
    printer.println(message)
}

interface BackgroundJobContext {

    val context: Context
    val printer: JobPrinter
    val isCancelled: Boolean

    fun quit()

}

class DefaultBackgroundJobContext(
        backingPrinter: PrintWriter,
        override val context: Context
) : BackgroundJobContext {

    override var printer: JobPrinter = PrintWriterJobPrinter(backingPrinter)
        private set

    var quitRequested = false

    private val atomicCancel = AtomicBoolean()

    override val isCancelled: Boolean
        get() = atomicCancel.get()

    override fun quit() {
        quitRequested = true
    }

    fun cancel() {
        atomicCancel.set(true)
        printer = JobPrinter.Nop
    }

}
