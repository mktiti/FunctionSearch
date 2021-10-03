package com.mktiti.fsearch.parser.intermediate.function

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.parser.intermediate.FunctionInfoCollector
import com.mktiti.fsearch.parser.intermediate.FunctionInfoResult
import com.mktiti.fsearch.parser.intermediate.TypeParamResolver
import com.mktiti.fsearch.parser.intermediate.asm.AsmFunctionInfoCollector
import com.mktiti.fsearch.parser.intermediate.parse.JarInfo
import com.mktiti.fsearch.parser.intermediate.type.JarInfoCollectorUtil

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

