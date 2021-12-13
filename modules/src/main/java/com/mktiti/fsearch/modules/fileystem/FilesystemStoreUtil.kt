package com.mktiti.fsearch.modules.fileystem

import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.util.resolveNested
import com.mktiti.fsearch.util.safeCutLast
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.streams.asSequence

internal object FilesystemStoreUtil {

    fun storedLocation(repoRoot: Path, id: ArtifactId): Path {
        return repoRoot.resolveNested(id.group + id.name + id.version)
    }

    fun findStoredArtifacts(repoRoot: Path): Set<ArtifactId> {
        return Files.walk(repoRoot).filter(Path::isDirectory)
                .asSequence()
                .map(repoRoot::relativize)
                .map { it.toList().map { d -> d.name } }
                .filterNotNull()
                .mapNotNull { subs ->
                    val (groupWithName, version) = subs.safeCutLast() ?: return@mapNotNull null
                    val (group, name) = groupWithName.safeCutLast() ?: return@mapNotNull null
                    if (group.isNotEmpty()) ArtifactId(group, name, version) else null
                }.toSet()
    }

}