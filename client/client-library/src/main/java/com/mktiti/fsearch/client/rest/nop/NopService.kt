package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.ArtifactApi
import com.mktiti.fsearch.client.rest.InfoApi
import com.mktiti.fsearch.client.rest.SearchApi
import com.mktiti.fsearch.client.rest.Service

object NopService : Service {

    override val address: String?
        get() = null

    override val searchApi: SearchApi
        get() = NopSearchApi

    override val infoApi: InfoApi
        get() = NopInfoApi

    override val artifactApi: ArtifactApi
        get() = NopArtifactApi

}