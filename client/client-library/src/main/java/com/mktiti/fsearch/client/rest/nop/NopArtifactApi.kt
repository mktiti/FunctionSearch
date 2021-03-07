package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.ArtifactApi
import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.ResultList

internal object NopArtifactApi : ArtifactApi {

    override fun all(): ApiCallResult<ResultList<ArtifactIdDto>> = nopResult()

    override fun load(id: ArtifactIdDto): ApiCallResult<Unit> = nopResult()

    override fun byGroup(group: String): ApiCallResult<ResultList<ArtifactIdDto>> = nopResult()

    override fun byName(group: String, name: String): ApiCallResult<ResultList<ArtifactIdDto>> = nopResult()

    override fun get(group: String, name: String, version: String): ApiCallResult<ArtifactIdDto?> = nopResult()

    override fun remove(group: String, name: String, version: String): ApiCallResult<Boolean> = nopResult()
}