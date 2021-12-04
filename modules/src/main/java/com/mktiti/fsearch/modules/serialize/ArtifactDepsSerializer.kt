package com.mktiti.fsearch.modules.serialize

import com.mktiti.fsearch.model.build.serialize.JacksonLineSerializer
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import com.mktiti.fsearch.model.build.service.SerializationException
import com.mktiti.fsearch.modules.ArtifactId
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories

object ArtifactDepsSerializer : ArtifactSerializerService<List<ArtifactId>> {

    private val serializer = JacksonLineSerializer.forClass<ArtifactId>()

    private fun Path.file(name: String) = resolve("$name-docs.jsonl").toFile()

    @Throws(IOException::class)
    override fun writeToDir(data: List<ArtifactId>, name: String, dir: Path) {
        dir.createDirectories()

        serializer.serializeToFile(data, dir.file(name))
    }

    @Throws(IOException::class, SerializationException::class)
    override fun readFromDir(dir: Path, name: String): List<ArtifactId> {
        return serializer.deserializeFromFile(dir.file(name))
    }

}

