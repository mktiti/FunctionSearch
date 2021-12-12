package com.mktiti.fsearch.model.build.service

import java.io.IOException
import java.nio.file.Path

class SerializationException(message: String) : RuntimeException(message)

interface ArtifactSerializerService<T> {

    @Throws(IOException::class)
    fun writeToDir(data: T, name: String, dir: Path)

    @Throws(IOException::class, SerializationException::class)
    fun readFromDir(dir: Path, name: String): T

}
