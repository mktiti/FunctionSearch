package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.parser.asm.AsmFunctionInfoCollector
import com.mktiti.fsearch.parser.service.indirect.FunctionInfoCollector
import com.mktiti.fsearch.parser.service.indirect.TypeParamResolver
import com.mktiti.fsearch.parser.type.DirectoryInfoCollectorUtil
import java.nio.file.Path

object DirectoryFunctionInfoCollector : FunctionInfoCollector<Path> {

    override fun collectFunctions(
            info: Path,
            infoRepo: JavaInfoRepo,
            typeParamResolver: TypeParamResolver
    ): FunctionInfoCollector.FunctionInfoCollection {
        return AsmFunctionInfoCollector.collect(infoRepo, typeParamResolver) {
            DirectoryInfoCollectorUtil.iterate(info, this)
        }
    }

}

