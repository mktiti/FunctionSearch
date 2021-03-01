package com.mktiti.fsearch.client.cli

import java.io.PrintWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

typealias BackgroundJob = BackgroundJobContext.() -> Unit

val NopBackgroundJob: BackgroundJob = { /* Nop */ }

fun printJob(message: String): BackgroundJob = {
    printer.println(message)
}

interface JobHandler {

    val hasActive: Boolean
    fun setJob(job: BackgroundJob)
    fun cancel()

}

class DefaultJobHandler(
        private val printer: PrintWriter,
        private val contextManager: ContextManager
) : JobHandler, AutoCloseable {

    private val executor = Executors.newSingleThreadExecutor()
    private val jobContainer = AtomicReference<DefaultBackgroundJobContext?>()

    override val hasActive: Boolean
        get() = jobContainer.get() != null

    override fun setJob(job: BackgroundJob) {
        with(DefaultBackgroundJobContext(printer, contextManager)) {
            val future = executor.submit {
                job()
            }
            jobContainer.set(this)
            future.get()
        }
    }

    override fun cancel() {
        jobContainer.get()?.cancel()
    }

    override fun close() {
        executor.shutdown()
    }

}

interface BackgroundJobContext {

    val contextManager: ContextManager
    val printer: PrintWriter
    val isCancelled: Boolean

}

class DefaultBackgroundJobContext(
        override val printer: PrintWriter,
        override val contextManager: ContextManager
) : BackgroundJobContext {

    private val atomicCancel = AtomicBoolean()

    override val isCancelled: Boolean
        get() = atomicCancel.get()

    fun cancel() {
        atomicCancel.set(true)
    }

}
