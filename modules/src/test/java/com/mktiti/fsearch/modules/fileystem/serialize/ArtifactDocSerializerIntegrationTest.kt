package com.mktiti.fsearch.modules.fileystem.serialize

import com.mktiti.fsearch.core.fit.FunIdParam.*
import com.mktiti.fsearch.core.fit.FunIdParam.Array
import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.javadoc.FunDocMap
import com.mktiti.fsearch.core.javadoc.FunctionDoc
import com.mktiti.fsearch.core.type.MinimalInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integrationTest")
internal class ArtifactDocSerializerIntegrationTest {

    @Test
    fun `test doc serialization-deserialization`() {
        val funDocMap = FunDocMap(mapOf(
                FunctionInfo(
                        file = MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                        relation = FunInstanceRelation.INSTANCE,
                        name = "testInstanceFun",
                        paramTypes = listOf(
                                TypeParam("T"),
                                Type(MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
                                Array(TypeParam("E")),
                        )
                ) to FunctionDoc(null, null, null, null),
                FunctionInfo(
                        file = MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeB"),
                        relation = FunInstanceRelation.CONSTRUCTOR,
                        name = "testConstructor",
                        paramTypes = listOf()
                ) to FunctionDoc(null, null, "short info", "long info"),
                FunctionInfo(
                        file = MinimalInfo(listOf("other", "package", "test"), "TestTypeC"),
                        relation = FunInstanceRelation.STATIC,
                        name = "testStaticFun",
                        paramTypes = listOf(
                                Array(Type(MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeP")))
                        )
                ) to FunctionDoc("http://javadoc.location/whatever", listOf("array"), "short info", "long info"),
        ))

        val asJson = ArtifactDocSerializer.serialize(funDocMap)
        val parsedResult = ArtifactDocSerializer.deserialize(asJson)

        assertEquals(funDocMap, parsedResult)
    }

}