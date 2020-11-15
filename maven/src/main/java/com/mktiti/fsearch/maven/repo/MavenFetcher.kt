package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.modules.ArtifactId
import java.io.File

interface MavenFetcher {

    fun <R> runOnArtifactWithDeps(artifacts: Collection<ArtifactId>, transform: (files: Map<ArtifactId, File>) -> R): R?

    fun <R> runOnArtifact(artifact: ArtifactId, transform: (file: File) -> R): R? = runOnArtifactWithDeps(listOf(artifact)) { files ->
        transform(files[artifact] ?: return@runOnArtifactWithDeps null)
    }

}