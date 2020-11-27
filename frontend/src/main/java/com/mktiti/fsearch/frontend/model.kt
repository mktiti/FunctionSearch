package com.mktiti.fsearch.frontend

data class ArtifactId(
        val group: String,
        val name: String,
        val version: String
) {

    fun dto() = ArtifactIdDto(group, name, version)

}

data class QueryContext(
        val artifacts: List<ArtifactId>
) {

    operator fun plus(artifact: ArtifactId) = copy(artifacts = artifacts + artifact)

    operator fun minus(artifact: ArtifactId) = copy(artifacts = artifacts - artifact)

    fun dto() = QueryCtxDto(artifacts.map(ArtifactId::dto))

}
