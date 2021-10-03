package com.mktiti.fsearch.modules

interface DependencyInfoFetcher {

    fun getDependencies(artifacts: Collection<ArtifactId>): Map<ArtifactId, Set<ArtifactId>>?

    fun getDependencies(artifact: ArtifactId): Map<ArtifactId, Set<ArtifactId>>? = getDependencies(listOf(artifact))

}