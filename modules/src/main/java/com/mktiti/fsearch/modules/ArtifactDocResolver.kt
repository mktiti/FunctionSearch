package com.mktiti.fsearch.modules

import com.mktiti.fsearch.core.javadoc.FunDocMap
import com.mktiti.fsearch.core.javadoc.FunDocResolver
import com.mktiti.fsearch.core.javadoc.SimpleMultiDocStore
import com.mktiti.fsearch.core.javadoc.SingleDocMapStore

interface ArtifactDocResolver {

    object Nop : ArtifactDocResolver {
        override fun forArtifacts(artifacts: Set<ArtifactId>) = FunDocResolver.nop()
    }

    operator fun get(artifact: ArtifactId) = forArtifact(artifact)

    operator fun get(artifacts: Set<ArtifactId>) = forArtifacts(artifacts)

    fun forArtifact(artifact: ArtifactId): FunDocResolver? = forArtifacts(setOf(artifact))

    fun forArtifacts(artifacts: Set<ArtifactId>): FunDocResolver?

    fun forArtifactsOrNop(artifacts: Set<ArtifactId>): FunDocResolver = forArtifacts(artifacts) ?: FunDocResolver.nop()

    fun forArtifactOrNop(artifact: ArtifactId): FunDocResolver = forArtifact(artifact) ?: FunDocResolver.nop()

}

interface ArtifactDocStore : ArtifactDocResolver {

    object Nop : ArtifactDocStore {
        override fun store(artifact: ArtifactId, docs: FunDocMap) {}

        override fun getData(artifact: ArtifactId): FunDocMap? = null

        override fun remove(artifact: ArtifactId) {}
    }

    fun store(artifact: ArtifactId, docs: FunDocMap)

    operator fun set(artifact: ArtifactId, docs: FunDocMap) = store(artifact, docs)

    fun getOrStore(id: ArtifactId, supplier: () -> FunDocMap): FunDocResolver {
        val data = getData(id) ?: supplier().also {
            store(id, it)
        }
        return wrapData(data)
    }

    fun getData(artifact: ArtifactId): FunDocMap?

    private fun wrapData(docMap: FunDocMap?): FunDocResolver {
        return if (docMap == null) {
            FunDocResolver.nop()
        } else {
            SingleDocMapStore(docMap.map)
        }
    }

    override fun forArtifact(artifact: ArtifactId): FunDocResolver {
        return wrapData(getData(artifact))
    }

    override fun forArtifacts(artifacts: Set<ArtifactId>): FunDocResolver {
        val resolvers = artifacts.map { wrapData(getData(it)) }
        return SimpleMultiDocStore(resolvers)
    }

    fun remove(artifact: ArtifactId)

    operator fun minusAssign(artifact: ArtifactId) = remove(artifact)

}