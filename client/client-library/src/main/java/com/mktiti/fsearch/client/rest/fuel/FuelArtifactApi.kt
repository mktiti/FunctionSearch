package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.RequestFactory
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.ArtifactApi
import com.mktiti.fsearch.dto.ArtifactIdDto

internal class FuelArtifactApi(
        private val fuel: RequestFactory.Convenience
) : ArtifactApi {

    private val artifactsPath = "artifacts"

    private fun path(group: String, artifact: String? = null, version: String? = null): String {
        val endPath = listOf(group, artifact, version)
                .takeWhile { it != null }
                .filterNotNull()
                .joinToString("/")

        return "$artifactsPath/$endPath"
    }

    override fun all(): ApiCallResult<Collection<ArtifactIdDto>> {
        return fuel.getJson(artifactsPath)
    }

    override fun load(id: ArtifactIdDto): ApiCallResult<Unit> {
        return fuel.postUnit(artifactsPath, id)
    }

    override fun byGroup(group: String): ApiCallResult<Collection<ArtifactIdDto>> {
        return fuel.getJson(path(group))
    }

    override fun byName(group: String, name: String): ApiCallResult<Collection<ArtifactIdDto>> {
        return fuel.getJson(path(group, name))
    }

    override fun get(group: String, name: String, version: String): ApiCallResult<ArtifactIdDto?> {
        return fuel.getJson(path(group, name, version))
    }

    override fun remove(group: String, name: String, version: String): ApiCallResult<Boolean> {
        return fuel.deleteBoolean(path(group, name, version))
    }
}