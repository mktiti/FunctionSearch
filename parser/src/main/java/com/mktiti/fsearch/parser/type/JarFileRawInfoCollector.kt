package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.parser.asm.AsmCollectorView
import com.mktiti.fsearch.parser.asm.AsmRawTypeInfoCollector
import com.mktiti.fsearch.parser.service.indirect.ArtifactTypeInfoCollector
import com.mktiti.fsearch.parser.service.indirect.RawTypeInfoResult
import java.nio.file.Path
import java.util.zip.ZipFile

object JarInfoCollectorUtil {

    fun iterate(info: JarFileInfoCollector.JarInfo, asmCollectorView: AsmCollectorView, sorted: Boolean) {
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
                        asmCollectorView.loadEntry(jar.getInputStream(entry))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

}

class JarFileInfoCollector(
        private val infoRepo: JavaInfoRepo
) : ArtifactTypeInfoCollector<JarFileInfoCollector.JarInfo> {

    data class JarInfo(
            val name: String,
            val paths: Collection<Path>
    ) {

        companion object {
            fun single(path: Path) = JarInfo(
                    name = path.fileName.toString(),
                    paths = listOf(path)
            )
        }

    }

    override fun collectRawInfo(info: JarInfo): RawTypeInfoResult {
        return AsmRawTypeInfoCollector.collect(infoRepo) {
            JarInfoCollectorUtil.iterate(info, this, sorted = false)
        }
    }

}
