package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.model.build.intermediate.TypeInfoResult
import com.mktiti.fsearch.model.build.service.ArtifactTypeInfoCollector
import com.mktiti.fsearch.parser.asm.AsmTypeInfoCollector
import com.mktiti.fsearch.parser.parse.JarInfo
import com.mktiti.fsearch.parser.util.JarInfoCollectorUtil

class JarFileInfoCollector(
        private val infoRepo: JavaInfoRepo
) : ArtifactTypeInfoCollector<JarInfo> {

    override fun collectTypeInfo(info: JarInfo): TypeInfoResult {
        return AsmTypeInfoCollector.collect(infoRepo) {
            JarInfoCollectorUtil.iterate(info, this, sorted = false)
        }
    }

}
