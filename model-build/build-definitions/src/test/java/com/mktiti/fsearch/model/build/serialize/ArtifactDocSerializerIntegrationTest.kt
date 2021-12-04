package com.mktiti.fsearch.model.build.serialize

import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.model.build.intermediate.IntFunIdParam.*
import com.mktiti.fsearch.model.build.intermediate.IntFunIdParam.Array
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files

@Tag("integrationTest")
internal class ArtifactDocSerializerIntegrationTest {

    companion object {
        private infix fun IntFunInfo.doc(doc: IntFunDoc) = IntFunDocEntry(this, doc)
    }

    @Test
    fun `test doc serialization-deserialization`() {
        val outDir = Files.createTempDirectory("fsearch-test-doc-serialize-out").apply {
            toFile().deleteOnExit()
        }

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
                ) doc IntFunDoc(null, null, null, null),
                IntFunInfo(
                        file = IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeB"),
                        relation = IntFunInstanceRelation.CONSTRUCTOR,
                        name = "testConstructor",
                        paramTypes = listOf()
                ) doc IntFunDoc(null, null, "short info", "long info"),
                IntFunInfo(
                        file = IntMinInfo(listOf("other", "package", "test"), "TestTypeC"),
                        relation = IntFunInstanceRelation.STATIC,
                        name = "testStaticFun",
                        paramTypes = listOf(
                                Array(Type(IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeP")))
                        )
                ) doc IntFunDoc("http://javadoc.location/whatever", listOf("array"), "short info", "long info"),
        ))

        ArtifactDocSerializer.writeToDir(funDocMap, "test", outDir)
        val parsedResult = ArtifactDocSerializer.readFromDir(outDir, "test")

        assertEquals(funDocMap, parsedResult)
    }

}
