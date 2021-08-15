package com.mktiti.fsearch.client.rest

import com.mktiti.fsearch.client.rest.fuel.FuelService
import com.mktiti.fsearch.client.rest.grpc.GrpcService

object ClientFactory {

    sealed class Config {

        data class RestConfig(
                val basePath: String
        ) : Config()

        data class GrpcConfig(
                val baseUrl: String,
                val port: Int
        ) : Config()

    }

    fun create(config: Config) = when (config) {
        is Config.RestConfig -> FuelService(config.basePath)
        is Config.GrpcConfig -> GrpcService(config.baseUrl, config.port)
    }

}