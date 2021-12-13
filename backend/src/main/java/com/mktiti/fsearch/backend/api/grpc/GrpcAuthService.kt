package com.mktiti.fsearch.backend.api.grpc

import com.mktiti.fsearch.backend.handler.AuthHandler
import com.mktiti.fsearch.grpc.Auth
import com.mktiti.fsearch.grpc.AuthServiceGrpc
import com.mktiti.fsearch.grpc.converter.toDto
import com.mktiti.fsearch.grpc.converter.toProto
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.beans.factory.annotation.Autowired

@GrpcService
class GrpcAuthService @Autowired constructor(
        private val backingHandler: AuthHandler
) : AuthServiceGrpc.AuthServiceImplBase() {

    override fun login(request: Auth.Credentials, responseObserver: StreamObserver<Auth.LoginResult>) {
        val result = backingHandler.login(request.toDto())
        responseObserver.response(result.toProto())
    }

}