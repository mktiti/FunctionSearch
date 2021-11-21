package com.mktiti.fsearch.modules.store

import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.docs.ArtifactDocStore

class ArtifactDocStoreWrapper(
        private val backingStore: GenericArtifactStore<FunDocMap>
) : ArtifactDocStore {

    override fun store(artifact: ArtifactId, docs: FunDocMap) {
        backingStore.store(artifact, docs)
    }

    override fun getData(artifact: ArtifactId): FunDocMap? {
        return backingStore.getData(artifact)
    }

    override fun remove(artifact: ArtifactId) {
        backingStore.remove(artifact)
    }

}