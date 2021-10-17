package com.mktiti.fsearch.modules

import com.mktiti.fsearch.core.repo.JavaRepo
import java.nio.file.Path

interface ArtifactManager {

    fun allStored(): Set<ArtifactId>

    fun effective(artifacts: Collection<ArtifactId>): Set<ArtifactId>

    fun getOrLoadJcl(version: String, paths: Collection<Path>): Pair<DomainRepo, JavaRepo>

    fun getSingle(artifact: ArtifactId): DomainRepo

    fun remove(artifact: ArtifactId): Boolean

    fun getWithDependencies(artifact: ArtifactId): DomainRepo
            = getWithDependencies(listOf(artifact))

    fun getWithDependencies(artifacts: Collection<ArtifactId>): DomainRepo

}

