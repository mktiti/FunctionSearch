package com.mktiti.fsearch.modules

interface ArtifactDepsResolver {

    sealed interface DependencyResult {
        data class AllFound(val dependencies: Set<ArtifactId>) : DependencyResult

        data class InfoMissing(
            val missingArtifacts: Set<ArtifactId>,
            val foundDependencies: Set<ArtifactId>
        ) : DependencyResult
    }

    fun dependencies(artifacts: Collection<ArtifactId>): DependencyResult

    fun dependencies(artifact: ArtifactId): Set<ArtifactId>?

}

interface ArtifactDepsStore : ArtifactDepsResolver {

    fun store(artifact: ArtifactId, dependencies: Set<ArtifactId>)

    fun store(dependencies: Map<ArtifactId, Set<ArtifactId>>) {
        dependencies.forEach { (artifact, deps) -> store(artifact, deps) }
    }

}
