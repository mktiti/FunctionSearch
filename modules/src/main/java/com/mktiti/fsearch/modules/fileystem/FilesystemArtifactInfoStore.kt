package com.mktiti.fsearch.modules.fileystem

import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.ArtifactInfoStore
import com.mktiti.fsearch.modules.fileystem.serialize.ArtifactInfoSerializer
import com.mktiti.fsearch.parser.intermediate.ArtifactInfoResult
import java.nio.file.Path

class FilesystemArtifactInfoStore(
        repoRoot: Path
) : ArtifactInfoStore {

    private val backingStore = GenericFilesystemArtifactStore(repoRoot, "info", "json", ArtifactInfoSerializer)

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