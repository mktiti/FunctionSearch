package com.mktiti.fsearch.backend.api

import com.mktiti.fsearch.dto.ArtifactIdDto

interface ArtifactHandler {

    object Nop : ArtifactHandler {
        override fun all(): Collection<ArtifactIdDto> = emptyList()

        override fun create(id: ArtifactIdDto) {}

        override fun byGroup(group: String): Collection<ArtifactIdDto> = emptyList()

        override fun byName(group: String, name: String): Collection<ArtifactIdDto> = emptyList()

        override fun get(group: String, name: String, version: String): ArtifactIdDto? = null

        override fun remove(group: String, name: String, version: String): Boolean = false
    }

    fun all(): Collection<ArtifactIdDto>

    fun create(id: ArtifactIdDto)

    fun byGroup(group: String): Collection<ArtifactIdDto>

    fun byName(group: String, name: String): Collection<ArtifactIdDto>

    fun get(group: String, name: String, version: String): ArtifactIdDto?

    fun remove(group: String, name: String, version: String): Boolean

}