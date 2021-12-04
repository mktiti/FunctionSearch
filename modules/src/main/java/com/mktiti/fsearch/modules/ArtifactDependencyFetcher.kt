package com.mktiti.fsearch.modules

interface ArtifactDependencyFetcher {

    fun dependencies(artifact: ArtifactId): Set<ArtifactId>?

    fun dependencies(artifacts: Collection<ArtifactId>): Map<ArtifactId, Set<ArtifactId>>?

}