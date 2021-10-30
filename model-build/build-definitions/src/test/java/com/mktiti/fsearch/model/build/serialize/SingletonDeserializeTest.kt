package com.mktiti.fsearch.model.build.serialize

import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.junit.jupiter.api.Test

internal class SingletonDeserializeTest {

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
    sealed class SealedWrapper {
        object NestedSingleton : SealedWrapper()
    }

    data class SealedData(
            val sealedMember: SealedWrapper
    )

    object SimpleSingleton

    data class SimpleData(
            val member: SimpleSingleton
    )

    @Test
    fun `test simple singleton deserialization`() {
        val serializer = JacksonSerializer.forClass<SimpleData>()
        val json = """
            {"member": {}}
        """.trimIndent()

        val result = serializer.deserialize(json)
        assert(result.member === SimpleSingleton)
    }

    @Test
    fun `test sealed singleton deserialization`() {
        val serializer = JacksonSerializer.forClass<SealedData>()
        val json = """
            {"sealedMember": 
                {"@c":".SingletonDeserializeTest${'$'}SealedWrapper${'$'}NestedSingleton"}
            }
        """.trimIndent()

        val result = serializer.deserialize(json)
        assert(result.sealedMember === SealedWrapper.NestedSingleton)
    }

}