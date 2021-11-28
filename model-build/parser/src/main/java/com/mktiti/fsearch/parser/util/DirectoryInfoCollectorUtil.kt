package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.parser.asm.AsmCollectorView
import java.io.FileInputStream
import java.nio.file.Path

object DirectoryInfoCollectorUtil {

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
                    e.printStackTrace()
                }
            }
        }
    }

}