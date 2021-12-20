package com.mktiti.fsearch.backend.api.grpc

import com.google.protobuf.Empty
import com.mktiti.fsearch.grpc.Admin
import com.mktiti.fsearch.grpc.AdminServiceGrpc
import com.mktiti.fsearch.grpc.converter.toProto
import com.mktiti.fsearch.rest.api.handler.AdminHandler
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.beans.factory.annotation.Autowired

@GrpcService
class GrpcAdminService @Autowired constructor(
        private val backingHandler: AdminHandler
) : AdminServiceGrpc.AdminServiceImplBase() {

    override fun searchStats(request: Empty, responseObserver: StreamObserver<Admin.SearchStatistics>) {
        val result = backingHandler.searchStats()
        responseObserver.response(result.toProto())
    }

}