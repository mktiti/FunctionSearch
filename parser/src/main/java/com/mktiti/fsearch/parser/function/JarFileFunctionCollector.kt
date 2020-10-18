package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.parser.asm.AsmFunctionCollector
import com.mktiti.fsearch.parser.service.FunctionCollector
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import java.util.zip.ZipFile

object JarFileFunctionCollector : FunctionCollector<JarFileInfoCollector.JarInfo> {

    override fun collectFunctions(info: JarFileInfoCollector.JarInfo, javaRepo: JavaRepo, infoRepo: JavaInfoRepo, dependencyResolver: TypeResolver): Collection<FunctionObj> {
        return AsmFunctionCollector.collect(javaRepo, infoRepo, dependencyResolver) {
            info.paths.forEach { jarPath ->
                ZipFile(jarPath.toFile()).use { jar ->
                    val entries = jar.entries().toList()

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

