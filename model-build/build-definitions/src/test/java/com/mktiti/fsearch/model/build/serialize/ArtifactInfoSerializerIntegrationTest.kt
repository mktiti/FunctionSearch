package com.mktiti.fsearch.model.build.serialize

import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.model.build.intermediate.IntFunIdParam.*
import com.mktiti.fsearch.model.build.intermediate.IntFunIdParam.Array
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integrationTest")
internal class ArtifactInfoSerializerIntegrationTest {

    @Test
    fun `test info serialization-deserialization`() {
        val artifactInfo = ArtifactInfoResult(
                typeInfo = TypeInfoResult(
                        directInfos = listOf(
                                SemiInfo.DirectInfo(
                                        info = IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                                        directSupers = listOf(
                                                IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeX"),
                                                IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeY")
                                        ),
                                        satSupers = listOf(
                                                IntStaticCmi(
                                                        base = IntMinInfo(listOf("com", "mktiti", "test"), "TestGenTypeX"),
                                                        args = listOf(
                                                                IntStaticCmi(IntMinInfo(listOf("java", "lang"), "String"), emptyList()),
                                                                IntStaticCmi(IntMinInfo(listOf("java", "lang", "number"), "Integer"), emptyList())
                                                        )
                                                )
                                        ),
                                        samType = null
                                ),
                                SemiInfo.DirectInfo(
                                        info = IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeB"),
                                        directSupers = listOf(),
                                        satSupers = listOf(
                                                IntStaticCmi(
                                                        base = IntMinInfo(listOf("package"), "A"),
                                                        args = listOf(
                                                                IntStaticCmi(IntMinInfo(listOf("package"), "String"), emptyList())
                                                        )
                                                ),
                                                IntStaticCmi(
                                                        base = IntMinInfo(listOf("package"), "List"),
                                                        args = listOf(
                                                                IntStaticCmi(IntMinInfo(listOf("package"), "Box"), listOf(
                                                                        IntStaticCmi(IntMinInfo(listOf("package"), "Box"), listOf(
                                                                                IntStaticCmi(IntMinInfo(listOf("package"), "box"), listOf(
                                                                                        IntStaticCmi(IntMinInfo(listOf("package"), "X"), listOf())
                                                                                ))
                                                                        ))
                                                                ))
                                                        )
                                                )
                                        ),
                                        samType = SamInfo.Direct(
                                                explicit = true,
                                                signature = FunSignatureInfo.Direct(
                                                        listOf(
                                                                "first" to IntStaticCmi(IntMinInfo(listOf(), "A"), listOf()),
                                                                "second" to IntStaticCmi(IntMinInfo(listOf(), "B"), listOf(
                                                                        IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                                ))
                                                        ),
                                                        IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                )
                                        )
                                )
                        ), templateInfos = listOf(
                            SemiInfo.TemplateInfo(
                                    info = IntMinInfo(listOf("my"), "TemplateType"),
                                    typeParams = listOf(
                                            TemplateTypeParamInfo("A", listOf(
                                                    TypeParamInfo.Wildcard,
                                                    TypeParamInfo.SelfRef,
                                                    TypeParamInfo.BoundedWildcard.UpperWildcard(TypeParamInfo.SelfRef),
                                                    TypeParamInfo.BoundedWildcard.LowerWildcard(
                                                            TypeParamInfo.Sat(
                                                                    IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                            )
                                                    ),
                                                    TypeParamInfo.Sat(IntStaticCmi(IntMinInfo(listOf(), "B"), listOf())),
                                                    TypeParamInfo.Direct(IntMinInfo(listOf(), "Direct")),
                                                    TypeParamInfo.Param(0)
                                            )), TemplateTypeParamInfo("B", emptyList())
                                    ),
                                    directSupers = listOf(
                                            IntMinInfo(listOf(), "DirectSuper")
                                    ),
                                    satSupers = listOf(
                                            IntStaticCmi(IntMinInfo(listOf(), "SatSuper"), listOf(
                                                    IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                            ))
                                    ),
                                    samType = SamInfo.Generic(
                                            explicit = true,
                                            signature = FunSignatureInfo.Generic(
                                                    typeParams = listOf(
                                                            TemplateTypeParamInfo("A", listOf(
                                                                    TypeParamInfo.Wildcard,
                                                                    TypeParamInfo.SelfRef,
                                                                    TypeParamInfo.BoundedWildcard.UpperWildcard(TypeParamInfo.SelfRef),
                                                                    TypeParamInfo.BoundedWildcard.LowerWildcard(
                                                                            TypeParamInfo.Sat(
                                                                                    IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                                            )
                                                                    ),
                                                                    TypeParamInfo.Sat(IntStaticCmi(IntMinInfo(listOf(), "B"), listOf())),
                                                                    TypeParamInfo.Direct(IntMinInfo(listOf(), "Direct")),
                                                                    TypeParamInfo.Param(0)
                                                            )), TemplateTypeParamInfo("B", emptyList())
                                                    ),
                                                    inputs = listOf(),
                                                    output = TypeParamInfo.Direct(IntMinInfo(listOf(), "Direct"))
                                            )
                                    ),
                                    datSupers = listOf(
                                            DatInfo(
                                                    template = IntMinInfo(listOf("java", "collections"), "Map"),
                                                    args = listOf(
                                                            TypeParamInfo.Direct(IntMinInfo(listOf("java", "lang"), "String")),
                                                            TypeParamInfo.Wildcard
                                                    )
                                            )
                                    )
                            )
                        )
                ),
                funInfo = FunctionInfoResult(
                        staticFunctions = listOf(
                                RawFunInfo.Direct(
                                        info = IntFunInfo(
                                                file = IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                                                relation = IntFunInstanceRelation.STATIC,
                                                name = "testInstanceFun",
                                                paramTypes = listOf(
                                                        TypeParam("T"),
                                                        Type(IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
                                                        Array(TypeParam("E")),
                                                )
                                        ),
                                        signature = FunSignatureInfo.Direct(
                                                listOf(
                                                        "first" to IntStaticCmi(IntMinInfo(listOf(), "A"), listOf()),
                                                        "second" to IntStaticCmi(IntMinInfo(listOf(), "B"), listOf(
                                                                IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                        ))
                                                ),
                                                IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                        )
                                )
                        ),
                        instanceMethods = listOf(
                                IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeA") to listOf(
                                        RawFunInfo.Direct(
                                                info = IntFunInfo(
                                                        file = IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                                                        relation = IntFunInstanceRelation.INSTANCE,
                                                        name = "testInstanceFun",
                                                        paramTypes = listOf(
                                                                TypeParam("T"),
                                                                Type(IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
                                                                Array(TypeParam("E")),
                                                        )
                                                ),
                                                signature = FunSignatureInfo.Direct(
                                                        listOf(
                                                                "first" to IntStaticCmi(IntMinInfo(listOf(), "A"), listOf()),
                                                                "second" to IntStaticCmi(IntMinInfo(listOf(), "B"), listOf(
                                                                        IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                                ))
                                                        ),
                                                        IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                )
                                        ),
                                        RawFunInfo.Generic(
                                                info = IntFunInfo(
                                                        file = IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                                                        relation = IntFunInstanceRelation.INSTANCE,
                                                        name = "testInstanceFun",
                                                        paramTypes = listOf(
                                                                TypeParam("T"),
                                                                Type(IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
                                                                Array(TypeParam("E")),
                                                        )
                                                ),
                                                signature = FunSignatureInfo.Generic(
                                                        typeParams = listOf(
                                                                TemplateTypeParamInfo("A", listOf(
                                                                        TypeParamInfo.Wildcard,
                                                                        TypeParamInfo.SelfRef,
                                                                        TypeParamInfo.BoundedWildcard.UpperWildcard(TypeParamInfo.SelfRef),
                                                                        TypeParamInfo.BoundedWildcard.LowerWildcard(
                                                                                TypeParamInfo.Sat(
                                                                                        IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                                                )
                                                                        ),
                                                                        TypeParamInfo.Sat(IntStaticCmi(IntMinInfo(listOf(), "B"), listOf())),
                                                                        TypeParamInfo.Direct(IntMinInfo(listOf(), "Direct")),
                                                                        TypeParamInfo.Param(0)
                                                                )), TemplateTypeParamInfo("B", emptyList())
                                                        ),
                                                        inputs = listOf(),
                                                        output = TypeParamInfo.Direct(IntMinInfo(listOf(), "Direct"))
                                                )
                                        )
                                ),
                                IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeB") to listOf(
                                        RawFunInfo.Direct(
                                                info = IntFunInfo(
                                                        file = IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeB"),
                                                        relation = IntFunInstanceRelation.INSTANCE,
                                                        name = "testInstanceFun2",
                                                        paramTypes = listOf(
                                                                TypeParam("T"),
                                                                Type(IntMinInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
                                                                Array(TypeParam("E")),
                                                        )
                                                ),
                                                signature = FunSignatureInfo.Direct(
                                                        listOf(
                                                                "first" to IntStaticCmi(IntMinInfo(listOf(), "A"), listOf()),
                                                                "second" to IntStaticCmi(IntMinInfo(listOf(), "B"), listOf(
                                                                        IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                                ))
                                                        ),
                                                        IntStaticCmi(IntMinInfo(listOf(), "A"), listOf())
                                                )
                                        )
                                )
                        )
                )
        )

        val asJson = ArtifactInfoSerializer.serialize(artifactInfo)
        val parsedResult = ArtifactInfoSerializer.deserialize(asJson)

        assertEquals(artifactInfo, parsedResult)
    }

}