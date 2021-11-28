package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.model.build.intermediate.TypeInfoResult
import com.mktiti.fsearch.model.build.service.ArtifactTypeInfoCollector
import com.mktiti.fsearch.parser.asm.AsmTypeInfoCollector
import com.mktiti.fsearch.parser.util.DirectoryInfoCollectorUtil
import java.nio.file.Path

class DirectoryInfoCollector(
        private val infoRepo: JavaInfoRepo
) : ArtifactTypeInfoCollector<Path> {

    override fun collectTypeInfo(info: Path): TypeInfoResult {
        return AsmTypeInfoCollector.collect(infoRepo) {
            DirectoryInfoCollectorUtil.iterate(info, this)
        }
    }

}
