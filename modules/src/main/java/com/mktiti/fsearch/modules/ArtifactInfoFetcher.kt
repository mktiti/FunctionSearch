package com.mktiti.fsearch.modules

import com.mktiti.fsearch.core.javadoc.FunDocMap
import com.mktiti.fsearch.parser.intermediate.ArtifactInfoResult

interface ArtifactInfoFetcher {

    fun fetchArtifact(artifactId: ArtifactId): ArtifactInfoResult? = fetchArtifacts(listOf(artifactId))?.singleOrNull()

    fun fetchArtifacts(artifactIds: List<ArtifactId>): List<ArtifactInfoResult>?

    fun fetchDoc(artifactId: ArtifactId): FunDocMap? = fetchDocs(listOf(artifactId))?.singleOrNull()

    fun fetchDocs(artifactIds: List<ArtifactId>): List<FunDocMap>?

}