package com.mktiti.fsearch.client.cli.context

import com.mktiti.fsearch.client.rest.Service
import com.mktiti.fsearch.client.rest.nop.NopService
import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.TypeDto

data class ContextImports(
        val importMap: Map<String, TypeDto>
) {

    companion object {
        fun empty() = ContextImports(emptyMap())
    }

    fun dto(): List<TypeDto> = importMap.values.toList()

    operator fun plus(type: TypeDto) = ContextImports(
            importMap + (type.simpleName to type)
    )

    operator fun minus(typeName: String) = ContextImports(
            importMap - typeName
    )

}

data class Context(
        val service: Service,
        val artifacts: Set<ArtifactIdDto>,
        val imports: ContextImports
) {

    companion object {
        fun empty() = Context(NopService, emptySet(), ContextImports.empty())
    }

    fun asDto(): QueryCtxDto = QueryCtxDto(artifacts.toList(), imports.dto())

    fun import(type: TypeDto) = copy(imports = imports + type)

    fun removeImport(typeName: String) = copy(imports = imports - typeName)

    operator fun plus(artifact: ArtifactIdDto) = copy(
            artifacts = artifacts + artifact
    )

    operator fun minus(artifact: ArtifactIdDto) = copy(
            artifacts = artifacts - artifact
    )

}
