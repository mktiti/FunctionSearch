package com.mktiti.fsearch.modules.store

import com.mktiti.fsearch.modules.ArtifactDepsResolver
import com.mktiti.fsearch.modules.ArtifactDepsResolver.DependencyResult.AllFound
import com.mktiti.fsearch.modules.ArtifactDepsResolver.DependencyResult.InfoMissing
import com.mktiti.fsearch.modules.ArtifactDepsStore
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.util.DependencyUtil
import com.mktiti.fsearch.util.splitMapKeep

class ArtifactDepsStoreWrapper(
        private val backingStore: GenericArtifactStore<List<ArtifactId>>
) : ArtifactDepsStore {

    override fun store(artifact: ArtifactId, dependencies: Set<ArtifactId>) {
        backingStore.store(artifact, dependencies.toList())
    }

    override fun dependencies(artifact: ArtifactId): Set<ArtifactId>? {
        return backingStore.getData(artifact)?.toSet()
    }

    override fun dependencies(artifacts: Collection<ArtifactId>): ArtifactDepsResolver.DependencyResult {
        val (foundDeps, missing) = artifacts.splitMapKeep {
            backingStore.getData(it)
        }

        val mergedDeps = DependencyUtil.mergeDependencies(foundDeps)

        return if (missing.isEmpty()) {
            AllFound(mergedDeps)
        } else {
            InfoMissing(missing.toSet(), mergedDeps)
        }
    }

}