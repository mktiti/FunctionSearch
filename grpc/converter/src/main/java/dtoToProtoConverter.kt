package com.mktiti.fsearch.grpc.converter

import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.grpc.Artifact
import com.mktiti.fsearch.grpc.Common
import com.mktiti.fsearch.grpc.Info
import com.mktiti.fsearch.grpc.Search

private fun TypeDto.toProto() = Common.Type.newBuilder()
        .setPackageName(packageName)
        .setSimpleName(simpleName)
        .build()

fun ArtifactIdDto.toProto(): Common.ArtifactId = Common.ArtifactId.newBuilder()
        .setGroup(group)
        .setName(name)
        .setVersion(version)
        .build()

private fun QueryCtxDto.toProto(): Common.QueryContext {
    return Common.QueryContext.newBuilder()
            .addAllImports(imports.map { it.toProto() })
            .addAllArtifactsIds(artifacts.map { it.toProto() })
            .build()
}

fun QueryRequestDto.toProto(): Search.QueryRequest = Search.QueryRequest.newBuilder()
        .setContext(context.toProto())
        .setQuery(query)
        .build()

fun infoRequest(queryContext: QueryCtxDto, namePart: String?): Info.InfoRequest = Info.InfoRequest.newBuilder().let {
    it.context = queryContext.toProto()
    if (namePart != null) {
       it.namePart = namePart
    }
    it.build()
}

private fun FunDocDto.toProto(): Search.FunDoc = Search.FunDoc.newBuilder().let {
    if (shortInfo != null) {
        it.shortInfo = shortInfo
    }
    if (details != null) {
        it.details = details
    }
    it.build()
}

private fun QueryFitResult.toProto() = Search.QueryFitResult.newBuilder().also { builder ->
    builder.file = file
    builder.funName = funName
    builder.header = header
    builder.doc = doc.toProto()
    builder.relation = relation.toProto()
}.build()

private fun QueryResult.Success.toProto() = Search.Success.newBuilder().apply {
    trimmed = results.trimmed
    val mappedResults = results.results.map {
        it.toProto()
    }
    addAllResults(mappedResults)
}.build()

private fun QueryResult.Error.toProto(): Search.Error {
    val type = when (this) {
        is QueryResult.Error.InternalError -> Search.Error.ErrorType.INTERNAL
        is QueryResult.Error.Query -> Search.Error.ErrorType.QUERY
    }

    return Search.Error.newBuilder()
            .setMessage(message)
            .setType(type)
            .build()
}

fun QueryResult.toProto(): Search.QueryResult = Search.QueryResult.newBuilder().apply {
    query = query
    when (this@toProto) {
        is QueryResult.Error -> error = toProto()
        is QueryResult.Success -> success = toProto()
    }
}.build()

fun TypeInfoDto.toProto(): Info.TypeInfo = Info.TypeInfo.newBuilder()
        .setType(type.toProto())
        .setTypeParamCount(typeParamCount)
        .build()

fun ResultList<TypeInfoDto>.toProto(): Info.TypeInfoResult = Info.TypeInfoResult.newBuilder()
        .setTrimmed(trimmed)
        .addAllResults(results.map { it.toProto() })
        .build()

fun FunRelationDto.toProto() = when (this) {
    FunRelationDto.STATIC -> Common.FunRelation.STATIC
    FunRelationDto.CONSTRUCTOR -> Common.FunRelation.CONSTRUCTOR
    FunRelationDto.INSTANCE -> Common.FunRelation.INSTANCE
}

fun FunId.toProto(): Info.FunInfo = Info.FunInfo.newBuilder()
        .setType(type.toProto())
        .setName(name)
        .setSignature(signature)
        .setRelation(relation.toProto())
        .build()

fun ResultList<FunId>.toProto(): Info.FunInfoResult = Info.FunInfoResult.newBuilder()
        .setTrimmed(trimmed)
        .addAllResults(results.map { it.toProto() })
        .build()


private fun HealthInfo.toProto() = Search.HealthInfo.newBuilder()
        .setVersion(version)
        .setBuildTimestamp(buildTimestamp)
        .setOk(ok)
        .build()

private fun artifactFilter(onBuilder: Artifact.ArtifactFilterMessage.Builder.() -> Unit): Artifact.ArtifactFilterMessage {
    return Artifact.ArtifactFilterMessage.newBuilder().apply(onBuilder).build()
}

fun allArtifacts() = artifactFilter {}

fun artifactFilter(group: String) = artifactFilter {
    setGroup(group)
}

fun artifactFilter(group: String, name: String) = artifactFilter {
    setGroup(group)
    setName(name)
}

fun artifactFilter(group: String, name: String, version: String): Artifact.ArtifactSelectMessage = Artifact.ArtifactSelectMessage.newBuilder()
        .setGroup(group)
        .setName(name)
        .setVersion(version)
        .build()

fun stringMessage(value: String): Common.StringMessage = Common.StringMessage.newBuilder()
        .setMessage(value)
        .build()

fun booleanMessage(value: Boolean): Common.BooleanMessage = Common.BooleanMessage.newBuilder()
        .setMessage(value)
        .build()

fun ResultList<ArtifactIdDto>.toProto(): Artifact.ArtifactListResult = Artifact.ArtifactListResult.newBuilder()
        .setTrimmed(trimmed)
        .addAllResults(results.map { it.toProto() })
        .build()

fun ArtifactIdDto?.protoGetResult(): Artifact.ArtifactGetResult = Artifact.ArtifactGetResult.newBuilder().let {
    if (this != null) {
        it.result = this.toProto()
    }
    it.build()
}