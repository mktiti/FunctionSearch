package com.mktiti.fsearch.modules.fileystem.serialize

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mktiti.fsearch.core.javadoc.FunDocMap
import kotlinx.serialization.SerializationException

object ArtifactDocSerializer : ArtifactSerializerService<FunDocMap> {

    private val mapper = jacksonObjectMapper()

    override fun serialize(data: FunDocMap): String {
        return mapper.writeValueAsString(data)
        //return json.encodeToString(data)
    }

    @Throws(SerializationException::class)
    override fun deserialize(string: String): FunDocMap {
        return mapper.readValue(string)
        //return json.decodeFromString(string)
    }

}