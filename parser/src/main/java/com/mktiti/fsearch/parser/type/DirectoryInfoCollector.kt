package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.parser.asm.AsmCollectorView
import com.mktiti.fsearch.parser.asm.AsmInfoCollector
import com.mktiti.fsearch.parser.service.IndirectInfoCollector
import java.io.FileInputStream
import java.nio.file.Path

object DirectoryCollectorUtil {

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
) : IndirectInfoCollector<Path> {

    override fun collectInitial(info: Path): IndirectInfoCollector.IndirectInitialData {
        return AsmInfoCollector.collect(infoRepo) {
            DirectoryCollectorUtil.iterate(info, this)
        }
    }

}
