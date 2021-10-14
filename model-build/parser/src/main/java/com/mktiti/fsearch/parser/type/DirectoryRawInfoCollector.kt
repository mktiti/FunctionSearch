package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.model.build.intermediate.TypeInfoResult
import com.mktiti.fsearch.model.build.service.ArtifactTypeInfoCollector
import com.mktiti.fsearch.parser.asm.AsmCollectorView
import com.mktiti.fsearch.parser.asm.AsmTypeInfoCollector
import java.io.FileInputStream
import java.nio.file.Path

object DirectoryInfoCollectorUtil {

    fun iterate(path: Path, asmCollectorView: AsmCollectorView) {
        path.toFile().walk().filter {
            it.extension == "class"
        }.forEach {
            FileInputStream(it).use(asmCollectorView::loadEntry)
        }
    }

}

class DirectoryInfoCollector(
        private val infoRepo: JavaInfoRepo
) : ArtifactTypeInfoCollector<Path> {

    override fun collectTypeInfo(info: Path): TypeInfoResult {
        return AsmTypeInfoCollector.collect(infoRepo) {
            DirectoryInfoCollectorUtil.iterate(info, this)
        }
    }

}
