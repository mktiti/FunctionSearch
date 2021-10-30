package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.model.build.intermediate.TypeInfoResult
import com.mktiti.fsearch.model.build.service.ArtifactTypeInfoCollector
import com.mktiti.fsearch.parser.asm.AsmCollectorView
import com.mktiti.fsearch.parser.asm.AsmTypeInfoCollector
import com.mktiti.fsearch.parser.parse.JarInfo
import java.util.zip.ZipFile

object JarInfoCollectorUtil {

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
) : ArtifactTypeInfoCollector<JarInfo> {

    override fun collectTypeInfo(info: JarInfo): TypeInfoResult {
        return AsmTypeInfoCollector.collect(infoRepo) {
            JarInfoCollectorUtil.iterate(info, this, sorted = false)
        }
    }

}
