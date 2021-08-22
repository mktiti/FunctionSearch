package com.mktiti.fsearch.parser.intermediate.function

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.parser.intermediate.FunctionInfoCollector
import com.mktiti.fsearch.parser.intermediate.TypeParamResolver
import com.mktiti.fsearch.parser.intermediate.asm.AsmFunctionInfoCollector
import com.mktiti.fsearch.parser.intermediate.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.intermediate.type.JarInfoCollectorUtil

object JarFileFunctionInfoCollector : FunctionInfoCollector<JarFileInfoCollector.JarInfo> {

    override fun collectFunctions(
            info: JarFileInfoCollector.JarInfo,
            infoRepo: JavaInfoRepo,
            typeParamResolver: TypeParamResolver
    ): FunctionInfoCollector.FunctionInfoCollection {
        return AsmFunctionInfoCollector.collect(infoRepo, typeParamResolver) {
            JarInfoCollectorUtil.iterate(info, this, sorted = false)
        }
    }

}

