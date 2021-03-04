package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.FuelManager
import com.mktiti.fsearch.client.rest.ArtifactApi
import com.mktiti.fsearch.client.rest.InfoApi
import com.mktiti.fsearch.client.rest.SearchApi
import com.mktiti.fsearch.client.rest.Service

class FuelService(
        basePath: String
) : Service {

    override val address: String?
        get() = fuelInstance.basePath

    private val fuelInstance = FuelManager().apply {
        this.basePath = basePath
    }

    override val searchApi: SearchApi = FuelSearchApi(fuelInstance)
    override val infoApi: InfoApi = FuelInfoApi(fuelInstance)
    override val artifactApi: ArtifactApi = FuelArtifactApi(fuelInstance)

}