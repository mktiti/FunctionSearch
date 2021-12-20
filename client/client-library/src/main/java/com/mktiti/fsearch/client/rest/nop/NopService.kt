package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.*

object NopService : Service {

    override val address: String?
        get() = null

    override val searchApi: SearchApi
        get() = NopSearchApi

    override val authApi: AuthApi
        get() = NopAuthApi

    override val infoApi: InfoApi
        get() = NopInfoApi

    override val artifactApi: ArtifactApi
        get() = NopArtifactApi

    override val userApi: UserApi
        get() = NopUserApi

    override val adminApi: AdminApi
        get() = NopAdminApi

}