package com.mktiti.fsearch.maven

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.TypeRepo

// TODO rework
interface ArtifactRepo {

    val info: MavenArtifact

    val typeRepo: TypeRepo

    val functions: Collection<FunctionObj>

}

data class SimpleArtifactRepo(
        override val info: MavenArtifact,
        override val typeRepo: TypeRepo,
        override val functions: Collection<FunctionObj>
) : ArtifactRepo
