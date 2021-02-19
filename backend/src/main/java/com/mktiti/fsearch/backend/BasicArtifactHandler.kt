package com.mktiti.fsearch.backend

import com.mktiti.fsearch.backend.api.ArtifactHandler
import com.mktiti.fsearch.backend.api.toId
import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.ArtifactManager

// TODO - efficient artifact filtering
class BasicArtifactHandler(
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

    private fun <T> Sequence<T>.limited() = take(50).toList()

    private fun rawAll(): Sequence<ArtifactIdDto> = artifactManager.allStored()
            .asSequence()
            .map { it.asDto() }

    override fun all(): Collection<ArtifactIdDto> = rawAll().limited()

    override fun create(id: ArtifactIdDto) {
        contextManager[ContextId(setOf(id.toId()))]
    }

    override fun byGroup(group: String): Collection<ArtifactIdDto> = rawAll().filter {
        it.group == group
    }.limited()

    override fun byName(group: String, name: String): Collection<ArtifactIdDto> = rawAll().filter {
        it.group == group && it.name == name
    }.limited()

    override fun get(group: String, name: String, version: String): ArtifactIdDto? = rawAll().find {
        it.group == group && it.name == name && it.version == version
    }

    override fun remove(group: String, name: String, version: String): Boolean {
        val id = ArtifactIdDto(group, name, version).toId()
        return artifactManager.remove(id)
    }
}