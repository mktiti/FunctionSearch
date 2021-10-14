package com.mktiti.fsearch.model.build.serialize

import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object ArtifactDocSerializer : ArtifactSerializerService<FunDocMap> {

    override fun serialize(data: FunDocMap): String {
        return Json.encodeToString(FunDocMap.serializer(), data)
    }

    @Throws(SerializationException::class)
    override fun deserialize(string: String): FunDocMap {
        return Json.decodeFromString(FunDocMap.serializer(), string)
    }

}