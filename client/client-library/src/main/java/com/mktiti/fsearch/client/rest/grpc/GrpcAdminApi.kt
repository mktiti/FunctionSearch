package com.mktiti.fsearch.client.rest.grpc

import com.google.protobuf.Empty
import com.mktiti.fsearch.client.rest.AdminApi
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.dto.SearchStatistics
import com.mktiti.fsearch.grpc.AdminServiceGrpc
import com.mktiti.fsearch.grpc.converter.toDto
import io.grpc.ManagedChannel

internal class GrpcAdminApi(
        channel: ManagedChannel
) : AdminApi {

    private val stub = AdminServiceGrpc.newBlockingStub(channel)

    override fun searchStats(): ApiCallResult<SearchStatistics> {
        return handleGrpc {
            stub.searchStats(Empty.getDefaultInstance()).toDto()
        }
    }

}