package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.model.build.intermediate.FunctionInfoResult
import com.mktiti.fsearch.model.build.service.FunctionInfoCollector
import com.mktiti.fsearch.model.build.service.TypeParamResolver
import com.mktiti.fsearch.parser.asm.AsmFunctionInfoCollector
import com.mktiti.fsearch.parser.parse.JarInfo
import com.mktiti.fsearch.parser.type.JarInfoCollectorUtil

class JarFileFunctionInfoCollector(
        private val infoRepo: JavaInfoRepo
) : FunctionInfoCollector<JarInfo> {

    override fun collectFunctions(
            info: JarInfo,
            typeParamResolver: TypeParamResolver
    ): FunctionInfoResult {
        return AsmFunctionInfoCollector.collect(infoRepo, typeParamResolver) {
            JarInfoCollectorUtil.iterate(info, this, sorted = false)
        }
    }

}

