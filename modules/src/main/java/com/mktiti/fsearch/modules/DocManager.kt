package com.mktiti.fsearch.modules

import com.mktiti.fsearch.core.javadoc.FunDocResolver
import com.mktiti.fsearch.core.javadoc.SimpleMultiDocStore
import com.mktiti.fsearch.core.javadoc.SingleDocMapStore
import com.mktiti.fsearch.core.util.zipIfSameLength
import com.mktiti.fsearch.util.splitMapKeep

interface DocManager {

    fun forArtifacts(artifactIds: Collection<ArtifactId>): FunDocResolver

}

class DefaultDocManager(
        private val jclDocs: FunDocResolver,
        private val cache: ArtifactDocStore,
        private val artifactInfoFetcher: ArtifactInfoFetcher
) : DocManager {

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