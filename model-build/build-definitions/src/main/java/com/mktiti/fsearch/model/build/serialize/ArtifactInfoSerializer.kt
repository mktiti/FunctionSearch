package com.mktiti.fsearch.model.build.serialize

import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object ArtifactInfoSerializer : ArtifactSerializerService<ArtifactInfoResult> {

    override fun serialize(data: ArtifactInfoResult): String {
        return Json.encodeToString(ArtifactInfoResult.serializer(), data)
    }

    @Throws(SerializationException::class)
    override fun deserialize(string: String): ArtifactInfoResult {
        return Json.decodeFromString(ArtifactInfoResult.serializer(), string)
    }

}