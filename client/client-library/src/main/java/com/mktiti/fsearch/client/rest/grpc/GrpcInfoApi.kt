package com.mktiti.fsearch.client.rest.grpc

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.InfoApi
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.dto.TypeInfoDto
import com.mktiti.fsearch.grpc.InfoServiceGrpc
import com.mktiti.fsearch.grpc.converter.infoRequest
import com.mktiti.fsearch.grpc.converter.toDto
import io.grpc.ManagedChannel

internal class GrpcInfoApi(
        channel: ManagedChannel
) : InfoApi {

    private val stub = InfoServiceGrpc.newBlockingStub(channel)

    override fun types(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<ResultList<TypeInfoDto>> {
        return handleGrpc {
            val result = stub.types(infoRequest(context, namePartOpt))
            result.toDto()
        }
    }

    override fun functions(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<ResultList<FunId>> {
        return handleGrpc {
            val result = stub.functions(infoRequest(context, namePartOpt))
            result.toDto()
        }
    }

}