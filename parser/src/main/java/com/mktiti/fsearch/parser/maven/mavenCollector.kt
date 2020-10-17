package com.mktiti.fsearch.parser.maven

import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.parser.function.JarFileFunctionCollector
import com.mktiti.fsearch.parser.service.CombinedCollector
import com.mktiti.fsearch.parser.service.FunctionCollector
import com.mktiti.fsearch.parser.type.IndirectJarTypeCollector
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.service.JarTypeCollector
import com.mktiti.fsearch.parser.util.JavaTypeParseLog
import java.io.File

class MavenCollector(
        private val repoInfo: MavenRepoInfo,
        log: JavaTypeParseLog,
        infoRepo: JavaInfoRepo
) : CombinedCollector<MavenArtifact> {

    /*
    private val backingTypeCollector: JarTypeCollector<JarFileInfoCollector.JarInfo> = TwoPhaseCollector(
            infoRepo = infoRepo,
            log = log,
            infoCollector = JarFileInfoCollector(infoRepo),
            connector = JavaTypeConnector(infoRepo, log)
    )
     */

    private val backingTypeCollector: JarTypeCollector<JarFileInfoCollector.JarInfo> = IndirectJarTypeCollector(infoRepo)

    private fun jarInfo(info: MavenArtifact, file: File) = JarFileInfoCollector.JarInfo(
            name = info.toString(),
            paths = listOf(file.toPath())
    )

    override fun collectCombined(info: MavenArtifact, javaRepo: JavaRepo, dependenyResolver: TypeResolver): CombinedCollector.Result {
        return MavenManager.onArtifact(repoInfo, info) { file ->
            val jarInfo = jarInfo(info, file)
            println(">>> Loading downloaded artifact $info")

            val typeRepo = backingTypeCollector.collectArtifact(jarInfo, javaRepo, dependenyResolver)
            val extendedResolver = FallbackResolver(typeRepo, dependenyResolver)
            val functions = JarFileFunctionCollector.collectFunctions(jarInfo, javaRepo, extendedResolver)

            CombinedCollector.Result(typeRepo, functions)
        }
    }

}
