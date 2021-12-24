package com.mktiti.fsearch.model.build.serialize

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature.SingletonSupport
import com.fasterxml.jackson.module.kotlin.KotlinFeature.StrictNullChecks
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.mktiti.fsearch.model.build.service.SerializationException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.streams.toList

class JacksonLineSerializer<T>(
        typeClass: Class<T>
) {

    private val reader = mapper.readerFor(typeClass)
    private val writer = mapper.writerFor(typeClass).withRootValueSeparator(System.lineSeparator())

    companion object {
        private val mapper = ObjectMapper().apply {
            val kotlin = kotlinModule {
                enable(StrictNullChecks)
                enable(SingletonSupport)
            }
            registerModule(kotlin)
        }

        inline fun <reified T> forClass() = JacksonLineSerializer(T::class.java)
    }

    @Throws(IOException::class)
    fun serializeToFile(data: List<T>, file: File) {
        writer.writeValues(file).use { writer ->
            writer.writeAll(data)
            writer.flush()
        }
    }

    @Throws(IOException::class)
    fun serializeToFile(data: Stream<T>, file: File) {
        writer.writeValues(file).use { writer ->
            data.sequential().forEach {
                writer.write(it)
            }
            writer.flush()
        }
    }

    fun serialize(data: T): String = mapper.writeValueAsString(data)

    @Throws(IOException::class, SerializationException::class)
    fun deserializeFromFile(file: File): List<T> = deserializeFromFileAsStream(file).toList()

    @Throws(IOException::class, SerializationException::class)
    fun deserializeFromFileAsStream(file: File): Stream<T> = Files.lines(file.toPath())
            .parallel()
            .map(this::deserializeLine)

    @Throws(SerializationException::class)
    fun deserializeLine(string: String): T = try {
        reader.readValue(string)
    } catch (jpe: JsonProcessingException) {
        throw SerializationException("Failed to deserialize data ($string)")
    } catch (jme: JsonMappingException) {
        throw SerializationException("Failed to deserialize data ($string)")
    }

}