package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.parser.asm.AsmCollectorView
import com.mktiti.fsearch.parser.asm.AsmRawTypeInfoCollector
import com.mktiti.fsearch.parser.service.indirect.ArtifactTypeInfoCollector
import com.mktiti.fsearch.parser.service.indirect.RawTypeInfoResult
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

    override fun collectRawInfo(info: Path): RawTypeInfoResult {
        return AsmRawTypeInfoCollector.collect(infoRepo) {
            DirectoryInfoCollectorUtil.iterate(info, this)
        }
    }

}
