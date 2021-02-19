package com.mktiti.fsearch.modules

interface ArtifactManager {

    fun allStored(): Set<ArtifactId>

    fun effective(artifacts: Collection<ArtifactId>): Set<ArtifactId>

    fun getSingle(artifact: ArtifactId): DomainRepo

    fun remove(artifact: ArtifactId): Boolean

    fun getWithDependencies(artifact: ArtifactId): DomainRepo
            = getWithDependencies(listOf(artifact))

    fun getWithDependencies(artifacts: Collection<ArtifactId>): DomainRepo

}
