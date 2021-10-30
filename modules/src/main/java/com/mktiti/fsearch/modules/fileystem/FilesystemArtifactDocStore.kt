package com.mktiti.fsearch.modules.fileystem

import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.serialize.ArtifactDocSerializer
import com.mktiti.fsearch.modules.ArtifactDocStore
import com.mktiti.fsearch.modules.ArtifactId
import java.nio.file.Path

class FilesystemArtifactDocStore(
        repoRoot: Path
) : ArtifactDocStore {

    private val backingStore = GenericFilesystemArtifactStore(repoRoot, "docs", "json", ArtifactDocSerializer)

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