package com.mktiti.fsearch.model.build.serialize

import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.intermediate.IntFunDocEntry
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import com.mktiti.fsearch.model.build.service.SerializationException
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories

object ArtifactDocSerializer : ArtifactSerializerService<FunDocMap> {

    private val serializer = JacksonLineSerializer.forClass<IntFunDocEntry>()

    private fun Path.file(name: String) = resolve("$name-docs.jsonl").toFile()

    @Throws(IOException::class)
    override fun writeToDir(data: FunDocMap, name: String, dir: Path) {
        dir.createDirectories()

        serializer.serializeToFile(data.map, dir.file(name))
    }

    @Throws(IOException::class, SerializationException::class)
    override fun readFromDir(dir: Path, name: String): FunDocMap {
        val docs = serializer.deserializeFromFile(dir.file(name))
        return FunDocMap(docs)
    }

}

