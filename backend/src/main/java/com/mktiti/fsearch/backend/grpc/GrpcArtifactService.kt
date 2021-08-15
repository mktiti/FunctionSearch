package com.mktiti.fsearch.backend.grpc

import com.google.protobuf.Empty
import com.mktiti.fsearch.backend.api.ArtifactHandler
import com.mktiti.fsearch.grpc.Artifact
import com.mktiti.fsearch.grpc.ArtifactServiceGrpc
import com.mktiti.fsearch.grpc.Common
import com.mktiti.fsearch.grpc.converter.*
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.beans.factory.annotation.Autowired

@GrpcService
class GrpcArtifactService @Autowired constructor(
        private val backingHandler: ArtifactHandler
) : ArtifactServiceGrpc.ArtifactServiceImplBase() {

    override fun load(request: Common.ArtifactId, responseObserver: StreamObserver<Empty>) {
       backingHandler.create(request.toDto())

        responseObserver.response(Empty.getDefaultInstance())
    }

    override fun remove(request: Common.ArtifactId, responseObserver: StreamObserver<Common.BooleanMessage>) {
        val isRemoved = backingHandler.remove(request.group, request.name, request.version)

        responseObserver.response(booleanMessage(isRemoved))
    }

    override fun list(request: Artifact.ArtifactFilterMessage, responseObserver: StreamObserver<Artifact.ArtifactListResult>) {
        val result = when (val filter = request.toDto()) {
            FilterMessageDto.AllFilter -> backingHandler.all()
            is FilterMessageDto.GroupFilter -> backingHandler.byGroup(filter.group)
            is FilterMessageDto.NameFilter -> backingHandler.byName(filter.group, filter.name)
        }

        responseObserver.response(result.toProto())
    }

    override fun get(request: Artifact.ArtifactSelectMessage, responseObserver: StreamObserver<Artifact.ArtifactGetResult>) {
        val id = request.toDto()
        val result = backingHandler.get(id.group, id.name, id.version)

        responseObserver.response(result.protoGetResult())
    }

}