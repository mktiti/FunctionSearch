package com.mktiti.fsearch.client.cli

import java.io.PrintWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

typealias BackgroundJob = BackgroundJobContext.() -> Unit

interface JobHandler {

    val hasActive: Boolean
    fun setJob(job: BackgroundJob)
    fun cancel()

}

class DefaultJobHandler(
        private val printer: PrintWriter
) : JobHandler {

    private val executor = Executors.newSingleThreadExecutor()
    private val jobContainer = AtomicReference<DefaultBackgroundJobContext?>()

    override val hasActive: Boolean
        get() = jobContainer.get() != null

    override fun setJob(job: BackgroundJob) {
        with(DefaultBackgroundJobContext(printer)) {
            job()
            executor.submit {
            }
            jobContainer.set(this)
        }
    }

    override fun cancel() {
        jobContainer.get()?.cancel()
    }

}

interface BackgroundJobContext {

    val printer: PrintWriter
    val isCancelled: Boolean

}

class DefaultBackgroundJobContext(
        override val printer: PrintWriter
) : BackgroundJobContext {

    private val atomicCancel = AtomicBoolean()

    override val isCancelled: Boolean
        get() = atomicCancel.get()

    fun cancel() {
        atomicCancel.set(true)
    }

}
