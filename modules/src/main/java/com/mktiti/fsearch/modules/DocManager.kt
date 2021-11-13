package com.mktiti.fsearch.modules

import com.mktiti.fsearch.core.javadoc.FunDocResolver
import com.mktiti.fsearch.core.javadoc.SimpleMultiDocStore
import com.mktiti.fsearch.core.javadoc.SingleDocMapStore
import com.mktiti.fsearch.core.util.zipIfSameLength
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.service.JarHtmlJavadocParser
import com.mktiti.fsearch.util.splitMapKeep
import java.nio.file.Path

interface DocManager {

    fun forArtifacts(artifactIds: Collection<ArtifactId>): FunDocResolver

    fun loadJclDocs(version: String, path: Path): FunDocResolver

}

class DefaultDocManager(
        private val cache: ArtifactDocStore,
        private val artifactInfoFetcher: ArtifactInfoFetcher,
        private val javadocParser: JarHtmlJavadocParser
) : DocManager {

    private var jclDocs = FunDocResolver.nop()

    override fun loadJclDocs(version: String, path: Path): FunDocResolver {
        return cache.getOrStore(ArtifactId.jcl(version)) {
            javadocParser.parseInput(path) ?: FunDocMap.empty()
        }.also {
            jclDocs = it
        }
    }

    override fun forArtifacts(artifactIds: Collection<ArtifactId>): FunDocResolver {
        val (storedResolvers, missingArtifacts) = artifactIds.splitMapKeep {
            cache[it]
        }

        val allResolvers: List<FunDocResolver> = storedResolvers + jclDocs + if (missingArtifacts.isNotEmpty()) {
            val funDocMaps = artifactInfoFetcher.fetchDocs(missingArtifacts)?.also {
                missingArtifacts.zipIfSameLength(it)?.forEach { (id, funDocMap) ->
                    cache.store(id, funDocMap)
                }
            } ?: error("Failed to fetch dependencies")

            funDocMaps.map { SingleDocMapStore(it.convertMap()) }
        } else {
            emptyList()
        }

        return SimpleMultiDocStore(allResolvers)
    }

}