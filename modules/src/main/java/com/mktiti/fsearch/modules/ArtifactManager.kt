package com.mktiti.fsearch.modules

interface ArtifactManager {

    fun effective(artifacts: Collection<ArtifactId>): Set<ArtifactId>

    fun getSingle(artifact: ArtifactId): DomainRepo

    fun getWithDependencies(artifact: ArtifactId): DomainRepo
            = getWithDependencies(listOf(artifact))

    fun getWithDependencies(artifacts: Collection<ArtifactId>): DomainRepo

}
