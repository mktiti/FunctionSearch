package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.parser.service.FunctionCollector
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import java.util.zip.ZipFile

class JarFileFunctionCollector(
        private val javaRepo: JavaRepo
) : FunctionCollector<JarFileInfoCollector.JarInfo> {

    override fun collectFunctions(info: JarFileInfoCollector.JarInfo, depsRepos: Collection<TypeRepo>): Collection<FunctionObj> {
        return AsmFunctionCollector.collect(javaRepo, depsRepos) {
            info.paths.forEach { jarPath ->
                ZipFile(jarPath.toFile()).use { jar ->
                    val entries = jar.entries().toList()
                    println("Entries in $jarPath (${entries.size}):")

                    entries.toList().filter {
                        it.name.endsWith(".class")
                    }.forEach { entry ->
                        loadEntry(jar.getInputStream(entry))
                    }
                }
            }
        }
    }

}

