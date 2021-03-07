package com.mktiti.fsearch.backend.api

import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.ResultList

interface ArtifactHandler {

    object Nop : ArtifactHandler {
        override fun all(): ResultList<ArtifactIdDto> = ResultList.empty()

        override fun create(id: ArtifactIdDto) {}

        override fun byGroup(group: String): ResultList<ArtifactIdDto> = ResultList.empty()

        override fun byName(group: String, name: String): ResultList<ArtifactIdDto> = ResultList.empty()

        override fun get(group: String, name: String, version: String): ArtifactIdDto? = null

        override fun remove(group: String, name: String, version: String): Boolean = false
    }

    fun all(): ResultList<ArtifactIdDto>

    fun create(id: ArtifactIdDto)

    fun byGroup(group: String): ResultList<ArtifactIdDto>

    fun byName(group: String, name: String): ResultList<ArtifactIdDto>

    fun get(group: String, name: String, version: String): ArtifactIdDto?

    fun remove(group: String, name: String, version: String): Boolean

}