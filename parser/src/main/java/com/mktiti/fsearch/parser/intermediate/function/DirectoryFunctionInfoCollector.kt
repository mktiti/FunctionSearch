package com.mktiti.fsearch.parser.intermediate.function

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.parser.intermediate.FunctionInfoCollector
import com.mktiti.fsearch.parser.intermediate.FunctionInfoResult
import com.mktiti.fsearch.parser.intermediate.TypeParamResolver
import com.mktiti.fsearch.parser.intermediate.asm.AsmFunctionInfoCollector
import com.mktiti.fsearch.parser.intermediate.type.DirectoryInfoCollectorUtil
import java.nio.file.Path

class DirectoryFunctionInfoCollector(
        private val infoRepo: JavaInfoRepo
) : FunctionInfoCollector<Path> {

    override fun collectFunctions(
            info: Path,
            typeParamResolver: TypeParamResolver
    ): FunctionInfoResult {
        return AsmFunctionInfoCollector.collect(infoRepo, typeParamResolver) {
            DirectoryInfoCollectorUtil.iterate(info, this)
        }
    }

}

