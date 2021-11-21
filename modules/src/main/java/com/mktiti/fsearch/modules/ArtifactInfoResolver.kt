package com.mktiti.fsearch.modules

import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult

interface ArtifactInfoResolver {

    @Suppress("unused")
    object Nop : ArtifactInfoResolver {
        override fun get(id: ArtifactId): Nothing? = null
    }

    operator fun get(id: ArtifactId): ArtifactInfoResult?

}

interface ArtifactInfoStore : ArtifactInfoResolver {

    @Suppress("unused")
    object Nop : ArtifactInfoStore {
        override fun get(id: ArtifactId): Nothing? = null

        override fun store(id: ArtifactId, info: ArtifactInfoResult) {}

        override fun remove(id: ArtifactId) {}
    }

    fun store(id: ArtifactId, info: ArtifactInfoResult)

    fun getOrStore(id: ArtifactId, supplier: () -> ArtifactInfoResult): ArtifactInfoResult {
        return get(id) ?: supplier().also {
            store(id, it)
        }
    }

    operator fun set(id: ArtifactId, info: ArtifactInfoResult) = store(id, info)

    fun remove(id: ArtifactId)

    operator fun minusAssign(id: ArtifactId) = remove(id)

}
