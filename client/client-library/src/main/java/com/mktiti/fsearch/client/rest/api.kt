package com.mktiti.fsearch.client.rest

import com.mktiti.fsearch.dto.*

interface Service {

    val address: String?

    val searchApi: SearchApi
    val infoApi: InfoApi
    val artifactApi: ArtifactApi

}

interface SearchApi {

    fun healthCheck(): ApiCallResult<MessageDto>

    fun search(context: QueryRequestDto): ApiCallResult<QueryResult>

}

interface InfoApi {

    fun types(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<ResultList<TypeInfoDto>>

    fun functions(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<ResultList<FunId>>

}

interface ArtifactApi {

    fun all(): ApiCallResult<ResultList<ArtifactIdDto>>

    fun load(id: ArtifactIdDto): ApiCallResult<Unit>

    fun byGroup(group: String): ApiCallResult<ResultList<ArtifactIdDto>>

    fun byName(group: String, name: String): ApiCallResult<ResultList<ArtifactIdDto>>

    fun get(group: String, name: String, version: String): ApiCallResult<ArtifactIdDto?>

    fun remove(group: String, name: String, version: String): ApiCallResult<Boolean>

}
