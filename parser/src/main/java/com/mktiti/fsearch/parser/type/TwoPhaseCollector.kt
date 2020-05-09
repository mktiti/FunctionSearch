package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.parser.util.JavaTypeParseLog
import com.mktiti.fsearch.util.MutablePrefixTree
import org.objectweb.asm.ClassReader
import java.io.InputStream
import java.lang.Exception
import java.nio.file.Path
import java.util.zip.ZipFile

data class JclResult(val javaRepo: JavaRepo, val typeRepo: TypeRepo)

interface JarTypeCollector {

    fun collectJcl(name: String, jarPath: Path): JclResult

    fun collectArtifact(name: String, jarPath: Path, javaRepo: JavaRepo): TypeRepo

}

private typealias InitialData<T> = MutablePrefixTree<String, T>

class TwoPhaseCollector(
        private val infoRepo: JavaInfoRepo,
        private val log: JavaTypeParseLog,
        private val connector: TypeConnector = JavaTypeConnector(infoRepo, log)
) : JarTypeCollector {

    private fun loadEntry(collector: InfoCollector, input: InputStream) {
        ClassReader(input).accept(collector, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
    }

    private fun collectInitial(name: String, jarPath: Path): Pair<InitialData<DirectCreator>, InitialData<TemplateCreator>> {
        val typeCollector = InfoCollector(name, infoRepo)

        ZipFile(jarPath.toFile()).use { jar ->
            val entries = jar.entries().toList()

            entries.toList().filter {
                it.name.endsWith(".class")
            }.sortedBy {
                it.name.removeSuffix(".class")
            }.forEach { entry ->
                try {
                    loadEntry(typeCollector, jar.getInputStream(entry))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return typeCollector.directTypes to typeCollector.templateTypes
    }

    override fun collectJcl(name: String, jarPath: Path): JclResult {
        val (directs, templates) = collectInitial(name, jarPath)
        return connector.connectJcl(directs, templates, name)
    }

    override fun collectArtifact(name: String, jarPath: Path, javaRepo: JavaRepo): TypeRepo {
        val (directs, templates) = collectInitial(name, jarPath)
        return connector.connectArtifact(directs, templates, javaRepo)
    }

}
