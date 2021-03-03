package com.mktiti.fsearch.client.cli.job

import java.io.PrintWriter

interface JobPrinter : AutoCloseable {

    object Nop : JobPrinter {
        override fun print(value: Any?) {}

        override fun print(string: String) {}

        override fun println(value: Any?) {}

        override fun println(string: String) {}

        override fun println() {}

        override fun flush() {}

        override fun close() {}
    }

    fun print(value: Any?)

    fun println(value: Any?)

    fun print(string: String)

    fun println(string: String)

    fun println()

    fun flush()

}

class PrintWriterJobPrinter(private val printer: PrintWriter) : JobPrinter {

    override fun print(value: Any?) {
        printer.print(value)
    }

    override fun print(string: String) {
        printer.print(string)
    }

    override fun println(value: Any?) {
        printer.println(value)
    }

    override fun println(string: String) {
        printer.println(string)
    }

    override fun println() {
        printer.println()
    }

    override fun flush() {
        printer.flush()
    }

    override fun close() {
        printer.close()
    }

}
