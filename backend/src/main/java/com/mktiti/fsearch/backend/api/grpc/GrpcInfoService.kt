package com.mktiti.fsearch.backend.api.grpc

import com.mktiti.fsearch.backend.handler.InfoHandler
import com.mktiti.fsearch.grpc.Info
import com.mktiti.fsearch.grpc.InfoServiceGrpc
import com.mktiti.fsearch.grpc.converter.toDto
import com.mktiti.fsearch.grpc.converter.toProto
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.beans.factory.annotation.Autowired

@GrpcService
class GrpcInfoService @Autowired constructor(
        private val backingHandler: InfoHandler
) : InfoServiceGrpc.InfoServiceImplBase() {

    override fun types(request: Info.InfoRequest, responseObserver: StreamObserver<Info.TypeInfoResult>) {
        val queryResult = backingHandler.types(request.context.toDto(), request.namePart)

        responseObserver.response(queryResult.toProto())
    }

    override fun functions(request: Info.InfoRequest, responseObserver: StreamObserver<Info.FunInfoResult>) {
        val queryResult = backingHandler.functions(request.context.toDto(), request.namePart)

        responseObserver.response(queryResult.toProto())
    }

}