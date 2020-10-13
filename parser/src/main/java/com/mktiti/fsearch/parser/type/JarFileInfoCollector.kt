package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.parser.service.IndirectInfoCollector
import com.mktiti.fsearch.parser.service.InfoCollector
import java.nio.file.Path
import java.util.zip.ZipFile

class JarFileInfoCollector(
        private val infoRepo: JavaInfoRepo
) : IndirectInfoCollector<JarFileInfoCollector.JarInfo> {

    data class JarInfo(
            val name: String,
            val paths: Collection<Path>
    )

    override fun collectInitial(info: JarInfo): IndirectInfoCollector.IndirectInitialData {
        return AsmInfoCollector.collect(info.name, infoRepo) {
            info.paths.forEach { jarPath ->
                ZipFile(jarPath.toFile()).use { jar ->
                    val entries = jar.entries().toList()

                    entries.toList().filter {
                        it.name.endsWith(".class")
                    }.sortedBy {
                        it.name.removeSuffix(".class")
                    }.forEach { entry ->
                        try {
                            loadEntry(jar.getInputStream(entry))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }


}
