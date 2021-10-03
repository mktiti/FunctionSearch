package com.mktiti.fsearch.modules.fileystem.serialize

import com.mktiti.fsearch.core.fit.FunIdParam.*
import com.mktiti.fsearch.core.fit.FunIdParam.Array
import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.parser.intermediate.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integrationTest")
internal class ArtifactInfoSerializerIntegrationTest {

    @Test
    fun `test info serialization-deserialization`() {
        val artifactInfo = ArtifactInfoResult(
                typeInfo = TypeInfoResult(
                        directInfos = setOf(
                                SemiInfo.DirectInfo(
                                        info = MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                                        directSupers = listOf(
                                                MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeX"),
                                                MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeY")
                                        ),
                                        satSupers = listOf(
                                                CompleteMinInfo.Static(
                                                        base = MinimalInfo(listOf("com", "mktiti", "test"), "TestGenTypeX"),
                                                        args = listOf(
                                                                CompleteMinInfo.Static(MinimalInfo(listOf("java", "lang"), "String"), emptyList()),
                                                                CompleteMinInfo.Static(MinimalInfo(listOf("java", "lang", "number"), "Integer"), emptyList())
                                                        )
                                                )
                                        ),
                                        samType = null
                                ),
                                SemiInfo.DirectInfo(
                                        info = MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeB"),
                                        directSupers = listOf(),
                                        satSupers = listOf(
                                                CompleteMinInfo.Static(
                                                        base = MinimalInfo(listOf("package"), "A"),
                                                        args = listOf(
                                                                CompleteMinInfo.Static(MinimalInfo(listOf("package"), "String"), emptyList())
                                                        )
                                                ),
                                                CompleteMinInfo.Static(
                                                        base = MinimalInfo(listOf("package"), "List"),
                                                        args = listOf(
                                                                CompleteMinInfo.Static(MinimalInfo(listOf("package"), "Box"), listOf(
                                                                        CompleteMinInfo.Static(MinimalInfo(listOf("package"), "Box"), listOf(
                                                                                CompleteMinInfo.Static(MinimalInfo(listOf("package"), "box"), listOf(
                                                                                        CompleteMinInfo.Static(MinimalInfo(listOf("package"), "X"), listOf())
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
                                                                "first" to CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf()),
                                                                "second" to CompleteMinInfo.Static(MinimalInfo(listOf(), "B"), listOf(
                                                                        CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                                                ))
                                                        ),
                                                        CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                                )
                                        )
                                )
                        ), templateInfos = listOf(
                            SemiInfo.TemplateInfo(
                                    info = MinimalInfo(listOf("my"), "TemplateType"),
                                    typeParams = listOf(
                                            TemplateTypeParamInfo("A", listOf(
                                                    TypeParamInfo.Wildcard,
                                                    TypeParamInfo.SelfRef,
                                                    TypeParamInfo.BoundedWildcard.UpperWildcard(TypeParamInfo.SelfRef),
                                                    TypeParamInfo.BoundedWildcard.LowerWildcard(
                                                            TypeParamInfo.Sat(
                                                                    CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                                            )
                                                    ),
                                                    TypeParamInfo.Sat(CompleteMinInfo.Static(MinimalInfo(listOf(), "B"), listOf())),
                                                    TypeParamInfo.Direct(MinimalInfo(listOf(), "Direct")),
                                                    TypeParamInfo.Param(0)
                                            )), TemplateTypeParamInfo("B", emptyList())
                                    ),
                                    directSupers = listOf(
                                            MinimalInfo(listOf(), "DirectSuper")
                                    ),
                                    satSupers = listOf(
                                            CompleteMinInfo.Static(MinimalInfo(listOf(), "SatSuper"), listOf(
                                                    CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
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
                                                                                    CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                                                            )
                                                                    ),
                                                                    TypeParamInfo.Sat(CompleteMinInfo.Static(MinimalInfo(listOf(), "B"), listOf())),
                                                                    TypeParamInfo.Direct(MinimalInfo(listOf(), "Direct")),
                                                                    TypeParamInfo.Param(0)
                                                            )), TemplateTypeParamInfo("B", emptyList())
                                                    ),
                                                    inputs = listOf(),
                                                    output = TypeParamInfo.Direct(MinimalInfo(listOf(), "Direct"))
                                            )
                                    ),
                                    datSupers = listOf(
                                            DatInfo(
                                                    template = MinimalInfo(listOf("java", "collections"), "Map"),
                                                    args = listOf(
                                                            TypeParamInfo.Direct(MinimalInfo(listOf("java", "lang"), "String")),
                                                            TypeParamInfo.Wildcard
                                                    )
                                            )
                                    )
                            )
                        )
                ),
                funInfo = FunctionInfoResult(
                        staticFunctions = setOf(
                                RawFunInfo.Direct(
                                        info = FunctionInfo(
                                                file = MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                                                relation = FunInstanceRelation.STATIC,
                                                name = "testInstanceFun",
                                                paramTypes = listOf(
                                                        TypeParam("T"),
                                                        Type(MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
                                                        Array(TypeParam("E")),
                                                )
                                        ),
                                        signature = FunSignatureInfo.Direct(
                                                listOf(
                                                        "first" to CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf()),
                                                        "second" to CompleteMinInfo.Static(MinimalInfo(listOf(), "B"), listOf(
                                                                CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                                        ))
                                                ),
                                                CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                        )
                                )
                        ),
                        instanceMethods = mapOf(
                                MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeA") to listOf(
                                        RawFunInfo.Direct(
                                                info = FunctionInfo(
                                                        file = MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                                                        relation = FunInstanceRelation.INSTANCE,
                                                        name = "testInstanceFun",
                                                        paramTypes = listOf(
                                                                TypeParam("T"),
                                                                Type(MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
                                                                Array(TypeParam("E")),
                                                        )
                                                ),
                                                signature = FunSignatureInfo.Direct(
                                                        listOf(
                                                                "first" to CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf()),
                                                                "second" to CompleteMinInfo.Static(MinimalInfo(listOf(), "B"), listOf(
                                                                        CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                                                ))
                                                        ),
                                                        CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                                )
                                        ),
                                        RawFunInfo.Generic(
                                                info = FunctionInfo(
                                                        file = MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeA"),
                                                        relation = FunInstanceRelation.INSTANCE,
                                                        name = "testInstanceFun",
                                                        paramTypes = listOf(
                                                                TypeParam("T"),
                                                                Type(MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
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
                                                                                        CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                                                                )
                                                                        ),
                                                                        TypeParamInfo.Sat(CompleteMinInfo.Static(MinimalInfo(listOf(), "B"), listOf())),
                                                                        TypeParamInfo.Direct(MinimalInfo(listOf(), "Direct")),
                                                                        TypeParamInfo.Param(0)
                                                                )), TemplateTypeParamInfo("B", emptyList())
                                                        ),
                                                        inputs = listOf(),
                                                        output = TypeParamInfo.Direct(MinimalInfo(listOf(), "Direct"))
                                                )
                                        )
                                ),
                                MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeB") to listOf(
                                        RawFunInfo.Direct(
                                                info = FunctionInfo(
                                                        file = MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeB"),
                                                        relation = FunInstanceRelation.INSTANCE,
                                                        name = "testInstanceFun2",
                                                        paramTypes = listOf(
                                                                TypeParam("T"),
                                                                Type(MinimalInfo(listOf("com", "mktiti", "test"), "TestTypeP")),
                                                                Array(TypeParam("E")),
                                                        )
                                                ),
                                                signature = FunSignatureInfo.Direct(
                                                        listOf(
                                                                "first" to CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf()),
                                                                "second" to CompleteMinInfo.Static(MinimalInfo(listOf(), "B"), listOf(
                                                                        CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
                                                                ))
                                                        ),
                                                        CompleteMinInfo.Static(MinimalInfo(listOf(), "A"), listOf())
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