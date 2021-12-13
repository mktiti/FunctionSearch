package com.mktiti.fsearch.backend.api.grpc

import com.google.protobuf.Empty
import com.mktiti.fsearch.backend.ProjectInfo
import com.mktiti.fsearch.backend.handler.SearchHandler
import com.mktiti.fsearch.grpc.Search
import com.mktiti.fsearch.grpc.SearchServiceGrpc.SearchServiceImplBase
import com.mktiti.fsearch.grpc.converter.toDto
import com.mktiti.fsearch.grpc.converter.toProto
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.beans.factory.annotation.Autowired

@GrpcService
class GrpcSearchService @Autowired constructor(
        private val backingHandler: SearchHandler
) : SearchServiceImplBase() {

    companion object {
        private val okHealth = Search.HealthInfo.newBuilder()
                .setVersion(ProjectInfo.version)
                .setBuildTimestamp(ProjectInfo.builtAt)
                .setOk(true)
                .build()
    }

    override fun search(request: Search.QueryRequest, responseObserver: StreamObserver<Search.QueryResult>) {
        val queryResult = backingHandler.syncQuery(request.context.toDto(), request.query)

        responseObserver.response(queryResult.toProto())
    }

    override fun healthCheck(request: Empty, responseObserver: StreamObserver<Search.HealthInfo>) {
        responseObserver.response(okHealth)
    }

}