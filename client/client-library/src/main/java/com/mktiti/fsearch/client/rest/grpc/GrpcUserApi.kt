package com.mktiti.fsearch.client.rest.grpc

import com.google.protobuf.Empty
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.UserApi
import com.mktiti.fsearch.dto.UserInfo
import com.mktiti.fsearch.grpc.UserServiceGrpc
import com.mktiti.fsearch.grpc.converter.toDto
import io.grpc.ManagedChannel

internal class GrpcUserApi(
        channel: ManagedChannel
) : UserApi {

    private val stub = UserServiceGrpc.newBlockingStub(channel)

    override fun selfData(): ApiCallResult<UserInfo> {
        return handleGrpc {
            stub.selfData(Empty.getDefaultInstance()).toDto()
        }
    }

}