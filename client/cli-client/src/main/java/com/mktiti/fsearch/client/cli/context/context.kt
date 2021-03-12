package com.mktiti.fsearch.client.cli.context

import com.mktiti.fsearch.client.rest.Service
import com.mktiti.fsearch.client.rest.nop.NopService
import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.TypeDto

data class Context(
        val service: Service,
        val artifacts: Set<ArtifactIdDto>,
        val imports: List<TypeDto>
) {

    companion object {
        fun empty() = Context(NopService, emptySet(), emptyList())
    }

    fun asDto(): QueryCtxDto = QueryCtxDto(artifacts.toList(), imports)

    fun import(type: TypeDto) = copy(imports = imports + type)

    fun removeImport(typeName: String) = copy(imports = imports.filterNot { it.simpleName == typeName })

    operator fun plus(artifact: ArtifactIdDto) = copy(
            artifacts = artifacts + artifact
    )

    operator fun minus(artifact: ArtifactIdDto) = copy(
            artifacts = artifacts - artifact
    )

}
