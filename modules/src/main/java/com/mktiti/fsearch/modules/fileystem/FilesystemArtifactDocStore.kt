package com.mktiti.fsearch.modules.fileystem

import com.mktiti.fsearch.core.javadoc.FunDocMap
import com.mktiti.fsearch.modules.ArtifactDocStore
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.fileystem.serialize.ArtifactDocSerializer
import java.io.IOException
import java.nio.file.Files
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