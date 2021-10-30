package com.mktiti.fsearch.model.build.service

import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class SerializationException(message: String) : RuntimeException(message)

interface ArtifactSerializerService<T> {

    @Throws(IOException::class)
    fun writeToFile(data: T, file: Path) {
        file.parent.createDirectories()
        file.writeText(serialize(data))
    }

    @Throws(IOException::class, SerializationException::class)
    fun readFromFile(file: Path): T {
        return deserialize(file.toFile().readText())
    }

    fun serialize(data: T): String

    @Throws(SerializationException::class)
    fun deserialize(string: String): T

}