package com.mktiti.fsearch.backend.api.grpc

import com.google.protobuf.Empty
import com.mktiti.fsearch.grpc.User
import com.mktiti.fsearch.grpc.UserServiceGrpc
import com.mktiti.fsearch.grpc.converter.toProto
import com.mktiti.fsearch.rest.api.handler.UserHandler
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.beans.factory.annotation.Autowired

@GrpcService
class GrpcUserService @Autowired constructor(
        private val backingHandler: UserHandler
) : UserServiceGrpc.UserServiceImplBase() {

    override fun selfData(request: Empty, responseObserver: StreamObserver<User.UserInfo>) {
        val result = backingHandler.selfData("")
        responseObserver.response(result.toProto())
    }

}