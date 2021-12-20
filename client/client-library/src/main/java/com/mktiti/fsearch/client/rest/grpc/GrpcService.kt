package com.mktiti.fsearch.client.rest.grpc

import com.mktiti.fsearch.client.rest.*
import io.grpc.ManagedChannelBuilder

internal class GrpcService(
        basePath: String,
        port: Int
) : Service {

    private val channel = ManagedChannelBuilder.forAddress(basePath, port).usePlaintext().build()

    override val address: String = "$basePath:$port"

    override val searchApi: SearchApi = GrpcSearchApi(channel)
    override val authApi: AuthApi = GrpcAuthApi(channel)
    override val infoApi: InfoApi = GrpcInfoApi(channel)
    override val artifactApi: ArtifactApi = GrpcArtifactApi(channel)
    override val userApi: UserApi = GrpcUserApi(channel)
    override val adminApi: AdminApi = GrpcAdminApi(channel)

}