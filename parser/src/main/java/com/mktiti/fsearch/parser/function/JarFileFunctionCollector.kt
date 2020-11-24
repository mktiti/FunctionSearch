package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.parser.asm.AsmFunctionCollector
import com.mktiti.fsearch.parser.service.FunctionCollector
import com.mktiti.fsearch.parser.type.JarCollectorUtil
import com.mktiti.fsearch.parser.type.JarFileInfoCollector

object JarFileFunctionCollector : FunctionCollector<JarFileInfoCollector.JarInfo> {

    override fun collectFunctions(
            info: JarFileInfoCollector.JarInfo,
            javaRepo: JavaRepo,
            infoRepo: JavaInfoRepo,
            dependencyResolver: TypeResolver
    ): FunctionCollector.FunctionCollection {
        return AsmFunctionCollector.collect(infoRepo, dependencyResolver) {
            JarCollectorUtil.iterate(info, this, true)
        }
    }

}

