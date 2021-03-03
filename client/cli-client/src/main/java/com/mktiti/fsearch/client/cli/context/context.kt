package com.mktiti.fsearch.client.cli.context

import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.TypeDto

data class ContextImports(
        val importMap: Map<String, TypeDto>
) {

    companion object {
        fun empty() = ContextImports(emptyMap())
    }

    operator fun plus(type: TypeDto) = ContextImports(
            importMap + (type.simpleName to type)
    )

    operator fun minus(typeName: String) = ContextImports(
            importMap - typeName
    )

}

data class Context(
        val artifacts: Set<ArtifactIdDto>,
        val imports: ContextImports
) {

    companion object {
        fun empty() = Context(emptySet(), ContextImports.empty())
    }

    val artifactsDto: QueryCtxDto
        get() = QueryCtxDto(artifacts.toList())

    fun import(type: TypeDto) = copy(imports = imports + type)

    fun removeImport(typeName: String) = copy(imports = imports - typeName)

    operator fun plus(artifact: ArtifactIdDto) = copy(
            artifacts = artifacts + artifact
    )

    operator fun minus(artifact: ArtifactIdDto) = copy(
            artifacts = artifacts - artifact
    )

}
