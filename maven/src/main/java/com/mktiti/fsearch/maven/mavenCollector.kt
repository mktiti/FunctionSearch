package com.mktiti.fsearch.maven

import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.maven.repo.ExternalMavenFetcher
import com.mktiti.fsearch.parser.function.JarFileFunctionCollector
import com.mktiti.fsearch.parser.service.CombinedCollector
import com.mktiti.fsearch.parser.type.IndirectJarTypeCollector
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.service.JarTypeCollector
import java.io.File

class MavenCollector(
        infoRepo: JavaInfoRepo
) {

    private val mavenManager = ExternalMavenFetcher()

    private val backingTypeCollector: JarTypeCollector<JarFileInfoCollector.JarInfo> = IndirectJarTypeCollector(infoRepo)

    private fun jarInfo(info: MavenArtifact, file: File) = JarFileInfoCollector.JarInfo(
            name = info.toString(),
            paths = listOf(file.toPath())
    )

    fun collectCombined(info: MavenArtifact, javaRepo: JavaRepo, infoRepo: JavaInfoRepo, dependencyResolver: TypeResolver): Collection<ArtifactRepo> {
        val result = mavenManager.fetchArtifactWithDeps(info) { currentInfo, file ->
            val jarInfo = jarInfo(currentInfo, file)
            println(">>> Loading downloaded artifact $currentInfo")

            val typeRepo = backingTypeCollector.collectArtifact(jarInfo, javaRepo, dependencyResolver)
            val extendedResolver = FallbackResolver(typeRepo, dependencyResolver)
            val functions = JarFileFunctionCollector.collectFunctions(jarInfo, javaRepo, infoRepo, extendedResolver)

            SimpleArtifactRepo(currentInfo, typeRepo, functions)
        } ?: error("Failed to fetch $info")

        return result.values
    }

}
