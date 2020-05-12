package com.mktiti.fsearch.parser.maven

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.parser.function.JarFileFunctionCollector
import com.mktiti.fsearch.parser.service.CombinedCollector
import com.mktiti.fsearch.parser.service.FunctionCollector
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.type.JarTypeCollector
import com.mktiti.fsearch.parser.type.JavaTypeConnector
import com.mktiti.fsearch.parser.type.TwoPhaseCollector
import com.mktiti.fsearch.parser.util.JavaTypeParseLog
import java.io.File

class MavenCollector(
        private val repoInfo: MavenRepoInfo,
        log: JavaTypeParseLog,
        infoRepo: JavaInfoRepo,
        private val javaRepo: JavaRepo
) : CombinedCollector<MavenArtifact> {

    private val backingTypeCollector: JarTypeCollector<JarFileInfoCollector.JarInfo> = TwoPhaseCollector(
            infoRepo = infoRepo,
            log = log,
            infoCollector = JarFileInfoCollector(infoRepo),
            connector = JavaTypeConnector(infoRepo, log)
    )

    private val backingFunctionCollector: FunctionCollector<JarFileInfoCollector.JarInfo> = JarFileFunctionCollector(
            javaRepo = javaRepo
    )

    private fun jarInfo(info: MavenArtifact, file: File) = JarFileInfoCollector.JarInfo(
            name = info.toString(),
            paths = listOf(file.toPath())
    )

    override fun collectCombined(info: MavenArtifact, depsRepo: Collection<TypeRepo>): CombinedCollector.Result {
        return MavenManager.onArtifact(repoInfo, info) { file ->
            val jarInfo = jarInfo(info, file)
            println(">>> Loading downloaded artifact $info")

            val typeRepo = backingTypeCollector.collectArtifact(jarInfo, javaRepo, depsRepo)
            val functions = backingFunctionCollector.collectFunctions(jarInfo, depsRepo + typeRepo)

            CombinedCollector.Result(typeRepo, functions)
        }
    }

}
