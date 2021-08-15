package com.mktiti.fsearch.client.rest.grpc

import com.google.protobuf.Empty
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.SearchApi
import com.mktiti.fsearch.dto.MessageDto
import com.mktiti.fsearch.dto.QueryRequestDto
import com.mktiti.fsearch.dto.QueryResult
import com.mktiti.fsearch.grpc.SearchServiceGrpc
import com.mktiti.fsearch.grpc.converter.toDto
import com.mktiti.fsearch.grpc.converter.toProto
import io.grpc.ManagedChannel

internal class GrpcSearchApi(
        channel: ManagedChannel
) : SearchApi {

    private val stub = SearchServiceGrpc.newBlockingStub(channel)

    override fun healthCheck(): ApiCallResult<MessageDto> {
        return handleGrpc {
            val message = stub.healthCheck(Empty.getDefaultInstance())
            MessageDto(message.message)
        }
    }

    override fun search(context: QueryRequestDto): ApiCallResult<QueryResult> {
        return handleGrpc {
            val message = stub.search(context.toProto())
            message.toDto()
        }
    }

}