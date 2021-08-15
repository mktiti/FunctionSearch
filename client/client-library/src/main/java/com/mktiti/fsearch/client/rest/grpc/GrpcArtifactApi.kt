package com.mktiti.fsearch.client.rest.grpc

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.ArtifactApi
import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.grpc.ArtifactServiceGrpc
import com.mktiti.fsearch.grpc.converter.allArtifacts
import com.mktiti.fsearch.grpc.converter.artifactFilter
import com.mktiti.fsearch.grpc.converter.toDto
import com.mktiti.fsearch.grpc.converter.toProto
import io.grpc.ManagedChannel

internal class GrpcArtifactApi(
        channel: ManagedChannel
) : ArtifactApi {

    private val stub = ArtifactServiceGrpc.newBlockingStub(channel)

    override fun all(): ApiCallResult<ResultList<ArtifactIdDto>> {
        return handleGrpc {
            stub.list(allArtifacts()).toDto()
        }
    }

    override fun byGroup(group: String): ApiCallResult<ResultList<ArtifactIdDto>> {
        return handleGrpc {
            stub.list(artifactFilter(group)).toDto()
        }
    }

    override fun byName(group: String, name: String): ApiCallResult<ResultList<ArtifactIdDto>> {
        return handleGrpc {
            stub.list(artifactFilter(group, name)).toDto()
        }
    }

    override fun get(group: String, name: String, version: String): ApiCallResult<ArtifactIdDto?> {
        return handleGrpc {
            stub.get(artifactFilter(group, name, version)).toDto()
        }
    }

    override fun load(id: ArtifactIdDto): ApiCallResult<Unit> {
        return handleGrpc {
            stub.load(id.toProto())
        }
    }

    override fun remove(group: String, name: String, version: String): ApiCallResult<Boolean> {
        return handleGrpc {
            val id = ArtifactIdDto(group, name, version).toProto()
            stub.remove(id).message
        }
    }
}
