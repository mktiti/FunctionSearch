package com.mktiti.fsearch.model.build.serialize

import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.model.build.intermediate.IntFunIdParam.*
import com.mktiti.fsearch.model.build.intermediate.IntFunIdParam.Array
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integrationTest")
internal class ArtifactDocSerializerIntegrationTest {

    @Test
    fun `test doc serialization-deserialization`() {
        val funDocMap = FunDocMap(listOf(
                IntFunInfo(
                        file = IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                        relation = IntFunInstanceRelation.INSTANCE,
                        name = "testInstanceFun",
                        paramTypes = listOf(
                                TypeParam("T"),
                                Type(IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
                                Array(TypeParam("E")),
                        )
                ) to IntFunDoc(null, null, null, null),
                IntFunInfo(
                        file = IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeB"),
                        relation = IntFunInstanceRelation.CONSTRUCTOR,
                        name = "testConstructor",
                        paramTypes = listOf()
                ) to IntFunDoc(null, null, "short info", "long info"),
                IntFunInfo(
                        file = IntMinInfo(listOf("other", "package", "test"), "TestTypeC"),
                        relation = IntFunInstanceRelation.STATIC,
                        name = "testStaticFun",
                        paramTypes = listOf(
                                Array(Type(IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeP")))
                        )
                ) to IntFunDoc("http://javadoc.location/whatever", listOf("array"), "short info", "long info"),
        ))

        val asJson = ArtifactDocSerializer.serialize(funDocMap)
        val parsedResult = ArtifactDocSerializer.deserialize(asJson)

        assertEquals(funDocMap, parsedResult)
    }

}