package com.mktiti.fsearch.client.rest.grpc

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.AuthApi
import com.mktiti.fsearch.dto.Credentials
import com.mktiti.fsearch.dto.LoginResult
import com.mktiti.fsearch.grpc.AuthServiceGrpc
import com.mktiti.fsearch.grpc.converter.toDto
import com.mktiti.fsearch.grpc.converter.toProto
import io.grpc.ManagedChannel

internal class GrpcAuthApi(
        channel: ManagedChannel
) : AuthApi {

    private val stub = AuthServiceGrpc.newBlockingStub(channel)

    override fun login(credentials: Credentials): ApiCallResult<LoginResult> {
        return handleGrpc {
            stub.login(credentials.toProto()).toDto()
        }
    }

}