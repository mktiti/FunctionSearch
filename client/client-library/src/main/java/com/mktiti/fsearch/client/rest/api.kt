package com.mktiti.fsearch.client.rest

import com.mktiti.fsearch.dto.*

interface Service {

    val address: String?

    val searchApi: SearchApi
    val authApi: AuthApi
    val infoApi: InfoApi
    val artifactApi: ArtifactApi

}

interface SearchApi {

    fun healthCheck(): ApiCallResult<HealthInfo>

    fun search(context: QueryRequestDto): ApiCallResult<QueryResult>

}

interface AuthApi {

    fun login(credentials: Credentials): ApiCallResult<LoginResult>

    fun login(username: String, password: String) = login(Credentials(username, password))

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
