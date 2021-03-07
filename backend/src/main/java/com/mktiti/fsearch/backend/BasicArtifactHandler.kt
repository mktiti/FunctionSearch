package com.mktiti.fsearch.backend

import com.mktiti.fsearch.backend.api.ArtifactHandler
import com.mktiti.fsearch.backend.api.limitedResult
import com.mktiti.fsearch.backend.api.toId
import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.ArtifactManager

// TODO - efficient artifact filtering
class BasicArtifactHandler(
        private val resultLimit: Int = 50,
        private val artifactManager: ArtifactManager,
        private val contextManager: ContextManager
) : ArtifactHandler {

    companion object {
        private fun ArtifactId.asDto() = ArtifactIdDto(
                group = group.joinToString("."),
                name = name,
                version = version
        )
    }

    private fun rawAll(): Sequence<ArtifactIdDto> = artifactManager.allStored()
            .asSequence()
            .map { it.asDto() }

    override fun all(): ResultList<ArtifactIdDto> = rawAll().limitedResult(resultLimit)

    override fun create(id: ArtifactIdDto) {
        contextManager[ContextId(setOf(id.toId()))]
    }

    override fun byGroup(group: String): ResultList<ArtifactIdDto> = rawAll().filter {
        it.group == group
    }.limitedResult(resultLimit)

    override fun byName(group: String, name: String): ResultList<ArtifactIdDto> = rawAll().filter {
        it.group == group && it.name == name
    }.limitedResult(resultLimit)

    override fun get(group: String, name: String, version: String): ArtifactIdDto? = rawAll().find {
        it.group == group && it.name == name && it.version == version
    }

    override fun remove(group: String, name: String, version: String): Boolean {
        val id = ArtifactIdDto(group, name, version).toId()
        return artifactManager.remove(id)
    }
}