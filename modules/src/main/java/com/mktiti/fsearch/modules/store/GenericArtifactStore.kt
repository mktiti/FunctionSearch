package com.mktiti.fsearch.modules.store

import com.mktiti.fsearch.modules.ArtifactId

interface GenericArtifactStore<T> {

    fun store(artifact: ArtifactId, data: T)

    fun getData(artifact: ArtifactId): T?

    fun remove(artifact: ArtifactId)

}