package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.parser.asm.AsmCollectorView
import com.mktiti.fsearch.parser.parse.JarInfo
import org.apache.logging.log4j.kotlin.logger
import java.util.zip.ZipFile

object JarInfoCollectorUtil {

    private val log = logger()

    fun iterate(info: JarInfo, asmCollectorView: AsmCollectorView, sorted: Boolean) {
        info.paths.forEach { jarPath ->
            ZipFile(jarPath.toFile()).use { jar ->
                val entries = jar.entries().toList().filter {
                    it.name.endsWith(".class")
                }

                val ordered = if (sorted) {
                    entries
                } else {
                    // Sorted so that nested (non-static) classes are parsed after their respective containers
                    entries.sortedBy {
                        it.name.removeSuffix(".class")
                    }
                }

                ordered.forEach { entry ->
                    try {
                        jar.getInputStream(entry).use {
                            asmCollectorView.loadEntry(it)
                        }
                    } catch (e: Exception) {
                        log.error("Error wile iterating over info JAR", e)
                    }
                }
            }
        }
    }

}