package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.parser.asm.AsmFunctionInfoCollector
import com.mktiti.fsearch.parser.service.indirect.FunctionInfoCollector
import com.mktiti.fsearch.parser.service.indirect.TypeParamResolver
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.type.JarInfoCollectorUtil

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

