package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.FuelManager
import com.mktiti.fsearch.client.rest.*

internal class FuelService(
        basePath: String
) : Service {

    override val address: String?
        get() = fuelInstance.basePath

    private val fuelInstance = FuelManager().apply {
        this.basePath = basePath
    }

    override val searchApi: SearchApi = FuelSearchApi(fuelInstance)
    override val authApi: AuthApi = FuelAuthApi(fuelInstance)
    override val infoApi: InfoApi = FuelInfoApi(fuelInstance)
    override val artifactApi: ArtifactApi = FuelArtifactApi(fuelInstance)
    override val userApi: UserApi = FuelUserApi(fuelInstance)
    override val adminApi: AdminApi = FuelAdminApi(fuelInstance)

}