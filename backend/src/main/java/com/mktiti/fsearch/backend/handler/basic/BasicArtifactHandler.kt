package com.mktiti.fsearch.backend.handler.basic

import com.mktiti.fsearch.backend.handler.ArtifactHandler
import com.mktiti.fsearch.backend.info.InfoService
import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.modules.ArtifactId

class BasicArtifactHandler(
        private val infoService: InfoService,
        private val resultLimit: Int = 50
) : ArtifactHandler {

    companion object {
        private fun ArtifactId.asDto() = ArtifactIdDto(
                group = group.joinToString("."),
                name = name,
                version = version
        )

        private fun String.asGroup() = split(".")

        private fun id(group: String, name: String, version: String) = ArtifactId(group.asGroup(), name, version)
    }

    private fun Sequence<ArtifactId>.result(): ResultList<ArtifactIdDto> = map { it.asDto() }.limitedResult(resultLimit)

    override fun all(): ResultList<ArtifactIdDto> = infoService.all().result()

    override fun create(id: ArtifactIdDto) = infoService.create(id.toId())

    override fun byGroup(group: String): ResultList<ArtifactIdDto> = infoService.byGroup(group.asGroup()).result()

    override fun byName(group: String, name: String): ResultList<ArtifactIdDto> = infoService
            .byName(group.asGroup(), name)
            .result()

    override fun get(group: String, name: String, version: String): ArtifactIdDto? {
        val id = id(group, name, version)
        return if (id in infoService) ArtifactIdDto(group, name, version) else null
    }

    override fun remove(group: String, name: String, version: String): Boolean {
        return infoService.remove(id(group, name, version))
    }
}