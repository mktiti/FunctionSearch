package com.mktiti.fsearch.modules.fileystem.serialize

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mktiti.fsearch.parser.intermediate.ArtifactInfoResult
import kotlinx.serialization.SerializationException

object ArtifactInfoSerializer : ArtifactSerializerService<ArtifactInfoResult> {

    private val mapper = jacksonObjectMapper()

    override fun serialize(data: ArtifactInfoResult): String {
        return mapper.writeValueAsString(data)
        // return json.encodeToString(ArtifactInfoResult.serializer(), data)
    }

    @Throws(SerializationException::class)
    override fun deserialize(string: String): ArtifactInfoResult {
        return mapper.readValue(string)
        //return json.decodeFromString(ArtifactInfoResult.serializer(), string)
    }

}