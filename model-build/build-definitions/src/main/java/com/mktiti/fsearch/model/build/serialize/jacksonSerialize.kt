package com.mktiti.fsearch.model.build.serialize

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature.SingletonSupport
import com.fasterxml.jackson.module.kotlin.KotlinFeature.StrictNullChecks
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.service.ArtifactSerializerService
import com.mktiti.fsearch.model.build.service.SerializationException
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.reader
import kotlin.io.path.writer

object ArtifactDocSerializer : ArtifactSerializerService<FunDocMap> by JacksonSerializer.forClass()
object ArtifactInfoSerializer: ArtifactSerializerService<ArtifactInfoResult> by JacksonSerializer.forClass()

internal class JacksonSerializer<T>(
        private val typeClass: Class<T>
) : ArtifactSerializerService<T> {

    companion object {
        private val mapper = ObjectMapper().apply {
            val kotlin = kotlinModule {
                enable(StrictNullChecks)
                enable(SingletonSupport)
            }
            registerModule(kotlin)
        }

        inline fun <reified T> forClass() = JacksonSerializer(T::class.java)
    }

    @Throws(IOException::class)
    override fun writeToFile(data: T, file: Path) {
        file.writer().use { fileWriter ->
            mapper.writeValue(fileWriter, data)
        }
    }

    @Throws(IOException::class, SerializationException::class)
    override fun readFromFile(file: Path): T {
        return file.reader().use { fileReader ->
            try {
                mapper.readValue(fileReader, typeClass)
            } catch (dbe: DatabindException) {
                throw SerializationException("Failed to deserialize data ($file)")
            } catch (sre: StreamReadException) {
                throw SerializationException("Failed to deserialize data ($file)")
            }
        }
    }

    override fun serialize(data: T): String = mapper.writeValueAsString(data)

    @Throws(SerializationException::class)
    override fun deserialize(string: String): T = try {
        mapper.readValue(string, typeClass)
    } catch (jpe: JsonProcessingException) {
        throw SerializationException("Failed to deserialize data ($string)")
    } catch (jme: JsonMappingException) {
        throw SerializationException("Failed to deserialize data ($string)")
    }

}