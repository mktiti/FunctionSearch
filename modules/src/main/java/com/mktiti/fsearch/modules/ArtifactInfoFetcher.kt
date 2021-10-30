package com.mktiti.fsearch.modules

import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.service.TypeParamResolver

interface ArtifactInfoFetcher {

    fun fetchArtifact(artifactId: ArtifactId, depTpResolver: TypeParamResolver): ArtifactInfoResult?
        = fetchArtifacts(listOf(artifactId), depTpResolver)?.singleOrNull()

    fun fetchArtifacts(artifactIds: List<ArtifactId>, depTpResolver: TypeParamResolver): List<ArtifactInfoResult>?

    fun fetchDoc(artifactId: ArtifactId): FunDocMap? = fetchDocs(listOf(artifactId))?.singleOrNull()

    fun fetchDocs(artifactIds: List<ArtifactId>): List<FunDocMap>?

}