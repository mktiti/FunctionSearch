package com.mktiti.fsearch.client.rest.grpc

import com.mktiti.fsearch.client.rest.ArtifactApi
import com.mktiti.fsearch.client.rest.InfoApi
import com.mktiti.fsearch.client.rest.SearchApi
import com.mktiti.fsearch.client.rest.Service
import io.grpc.ManagedChannelBuilder

internal class GrpcService(
        basePath: String,
        port: Int
) : Service {

    private val channel = ManagedChannelBuilder.forAddress(basePath, port).usePlaintext().build()

    override val address: String = "$basePath:$port"

    override val searchApi: SearchApi = GrpcSearchApi(channel)
    override val infoApi: InfoApi = GrpcInfoApi(channel)
    override val artifactApi: ArtifactApi = GrpcArtifactApi(channel)

}