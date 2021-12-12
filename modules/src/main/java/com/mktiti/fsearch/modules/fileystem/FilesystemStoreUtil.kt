package com.mktiti.fsearch.modules.fileystem

import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.util.resolveNested
import java.nio.file.Path

internal object FilesystemStoreUtil {

    fun storedLocation(repoRoot: Path, id: ArtifactId): Path {
        return repoRoot.resolveNested(id.group + id.name + id.version)
    }

}