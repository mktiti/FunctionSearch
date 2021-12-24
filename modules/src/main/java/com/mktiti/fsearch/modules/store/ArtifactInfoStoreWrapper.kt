package com.mktiti.fsearch.modules.store

import ArtifactInfoSeqResult
import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.ArtifactInfoStore

class ArtifactInfoStoreWrapper(
        private val backingStore: GenericArtifactStore<ArtifactInfoResult>
) : ArtifactInfoStore {

    override fun get(id: ArtifactId): ArtifactInfoResult? {
        return backingStore.getData(id)
    }

    override fun store(id: ArtifactId, info: ArtifactInfoResult) {
        backingStore.store(id, info)
    }

    override fun remove(id: ArtifactId) {
        backingStore.remove(id)
    }

}

class ArtifactInfoSeqStoreWrapper(
        private val backingStore: GenericArtifactStore<ArtifactInfoSeqResult>
) : ArtifactInfoStore {

    override fun get(id: ArtifactId): ArtifactInfoResult? {
        return backingStore.getData(id)?.collect()
    }

    override fun getSeq(id: ArtifactId): ArtifactInfoSeqResult? {
        return backingStore.getData(id)
    }

    override fun store(id: ArtifactId, info: ArtifactInfoResult) {
        backingStore.store(id, info.toSeq())
    }

    override fun remove(id: ArtifactId) {
        backingStore.remove(id)
    }

}