package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.model.build.intermediate.FunctionInfoResult
import com.mktiti.fsearch.model.build.service.FunctionInfoCollector
import com.mktiti.fsearch.model.build.service.TypeParamResolver
import com.mktiti.fsearch.parser.asm.AsmFunctionInfoCollector
import com.mktiti.fsearch.parser.type.DirectoryInfoCollectorUtil
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

