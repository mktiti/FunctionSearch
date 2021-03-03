package com.mktiti.fsearch.client.cli.job

import com.mktiti.fsearch.client.cli.context.Context
import com.mktiti.fsearch.client.cli.context.ContextManager
import java.io.PrintWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

interface JobHandler {

    val hasActive: Boolean

    fun runJob(job: BackgroundJob): Boolean

    fun cancel(): Boolean

}

class DefaultJobHandler(
        private val printer: PrintWriter,
        private val contextManager: ContextManager
) : JobHandler, AutoCloseable {

    private val executor = Executors.newFixedThreadPool(2)
    private val jobContainer = AtomicReference<DefaultBackgroundJobContext?>()

    override val hasActive: Boolean
        get() = jobContainer.get() != null

    override fun runJob(job: BackgroundJob): Boolean {
        return with(DefaultBackgroundJobContext(printer, contextManager.context)) {
            if (jobContainer.compareAndSet(null, this)) {
                val future = executor.submit<Context> {
                    job()
                }
                val result = future.get()
                printer.flush()
                if (jobContainer.compareAndSet(this, null)) {
                    contextManager.context = result
                }
                quitRequested
            } else {
                // Don't quit
                false
            }
        }
    }

    override fun cancel(): Boolean {
        return jobContainer.getAndSet(null)?.also {
            it.cancel()
        } != null
    }

    override fun close() {
        executor.shutdown()
    }

}