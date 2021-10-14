package com.mktiti.fsearch.modules

import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.intermediate.FunDocMap

interface ArtifactInfoFetcher {

    fun fetchArtifact(artifactId: ArtifactId): ArtifactInfoResult? = fetchArtifacts(listOf(artifactId))?.singleOrNull()

    fun fetchArtifacts(artifactIds: List<ArtifactId>): List<ArtifactInfoResult>?

    fun fetchDoc(artifactId: ArtifactId): FunDocMap? = fetchDocs(listOf(artifactId))?.singleOrNull()

    fun fetchDocs(artifactIds: List<ArtifactId>): List<FunDocMap>?

}