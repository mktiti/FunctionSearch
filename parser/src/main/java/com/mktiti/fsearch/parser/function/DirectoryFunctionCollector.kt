package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.parser.asm.AsmFunctionCollector
import com.mktiti.fsearch.parser.service.FunctionCollector
import com.mktiti.fsearch.parser.type.DirectoryCollectorUtil
import java.nio.file.Path

object DirectoryFunctionCollector : FunctionCollector<Path> {

    override fun collectFunctions(info: Path, javaRepo: JavaRepo, infoRepo: JavaInfoRepo, dependencyResolver: TypeResolver): Collection<FunctionObj> {
        return AsmFunctionCollector.collect(infoRepo, dependencyResolver) {
            DirectoryCollectorUtil.iterate(info, this)
        }
    }

}

