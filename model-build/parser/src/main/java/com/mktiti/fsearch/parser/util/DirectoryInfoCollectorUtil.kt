package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.parser.asm.AsmCollectorView
import com.mktiti.fsearch.util.logger
import java.io.FileInputStream
import java.nio.file.Path

object DirectoryInfoCollectorUtil {

    private val log = logger()

    fun iterate(path: Path, asmCollectorView: AsmCollectorView) {
        path.toFile().walk().filter {
            it.extension == "class"
        }.sortedBy {
            it.nameWithoutExtension
        }.forEach {
            FileInputStream(it).use { stream ->
                try {
                    asmCollectorView.loadEntry(stream)
                } catch (e: Exception) {
                    log.error("Error while iterating info files", e)
                }
            }
        }
    }

}