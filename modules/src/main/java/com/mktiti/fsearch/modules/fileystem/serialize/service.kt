package com.mktiti.fsearch.modules.fileystem.serialize

import kotlinx.serialization.SerializationException
import java.io.IOException
import java.nio.file.Path

interface ArtifactSerializerService<T> {

    @Throws(IOException::class)
    fun writeToFile(data: T, file: Path) {
        file.toFile().writeText(serialize(data))
    }

    @Throws(IOException::class)
    fun readFromFile(file: Path): T {
        return deserialize(file.toFile().readText())
    }

    fun serialize(data: T): String

    @Throws(SerializationException::class)
    fun deserialize(string: String): T

}
