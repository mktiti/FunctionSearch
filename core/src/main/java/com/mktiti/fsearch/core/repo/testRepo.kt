@file:Suppress("UNUSED_VARIABLE")

package com.mktiti.fsearch.core.repo // For easier extension

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.Bounded.BoundDirection.LOWER
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.forceDynamicApply
import com.mktiti.fsearch.core.util.forceStaticApply
import com.mktiti.fsearch.core.util.listType

fun createTestRepo(): MutableTypeRepo {

    val rootType = DirectType(
            minInfo = MinimalInfo(emptyList(), "TestRoot"),
            superTypes = emptyList(),
            samType = null,
            virtual = false
    )

    val defaultTypeBounds = TypeBounds(
            upperBounds = setOf(TypeSubstitution(rootType.holder()))
    )

    return SetTypeRepo(
            // funTypeInfo = TypeInfo("TestFun", emptyList(), ""),
            // rootInfo = TypeInfo("TestRoot", emptyList(), "")
    ).apply {

        this += rootType

        val comparable = createTemplate(
            fullName = "Comparable",
            typeParams = listOf(TypeParameter("C", defaultTypeBounds)),
            superTypes = listOf(rootType)
        )

        val collection = createTemplate(
            fullName = "Collection",
            typeParams = listOf(TypeParameter("E", defaultTypeBounds)),
            superTypes = listOf(rootType)
        )

        val list = createTemplate(
            fullName = "List",
            typeParams = listOf(TypeParameter("T", defaultTypeBounds)),
            superTypes = listOf(collection.forceDynamicApply(ParamSubstitution(0)))
        )

        fun comparableTo(name: String): TypeHolder.Static {
            return comparable.forceStaticApply(TypeHolder.staticIndirects(
                    CompleteMinInfo.Static(
                            base = MinimalInfo(emptyList(), name),
                            args = emptyList()
                    )
            )).holder()
        }

        val int = createDirect(
            fullName = "Int",
            superTypes = listOf(
                rootType.holder(),
                comparableTo("Int")
            )
        )

        val int64 = createDirect("Int64", int)

        val char = createDirect("Char", rootType)

        val charSeq = createDirect(
                "CharSequence",
                list.forceStaticApply(char.holder())
        )

        val string = createDirect(
            fullName = "String",
            superTypes = listOf(
                rootType.holder(),
                comparableTo("String"),
                charSeq.holder()
            )
        )

        val linkedList = createTemplate(
            fullName = "LinkedList",
            typeParams = listOf(TypeParameter("T", defaultTypeBounds)),
            superTypes = listOf(listType.forceDynamicApply(ParamSubstitution(0)))
        )

        val arrayList = createTemplate(
            fullName = "ArrayList",
            typeParams = listOf(TypeParameter("T", defaultTypeBounds)),
            superTypes = listOf(listType.forceDynamicApply(ParamSubstitution(0)))
        )

        val pair = createTemplate(
            fullName = "Pair",
            typeParams = listOf(
                    TypeParameter("L", defaultTypeBounds),
                    TypeParameter("R", defaultTypeBounds)
            ),
            superTypes = listOf(rootType)
        )

        val map = createTemplate(
            fullName = "Map",
            typeParams = listOf(
                    TypeParameter("K", defaultTypeBounds),
                    TypeParameter("V", defaultTypeBounds)
            ),
            superTypes = listOf(
                collection.forceDynamicApply(
                    TypeSubstitution(
                        pair.forceDynamicApply(
                            ParamSubstitution(0),
                            ParamSubstitution(1)
                        ).holder()
                    )
                )
            )
        )

        val hashMap = createTemplate(
            fullName = "HashMap",
            typeParams = listOf(
                    TypeParameter("K", defaultTypeBounds),
                    TypeParameter("V", defaultTypeBounds)
            ),
            superTypes = listOf(
                map.forceDynamicApply(
                    ParamSubstitution(0), ParamSubstitution(1)
                )
            )
        )

        val orderedMap = createTemplate(
            fullName = "OrderedMap",
            typeParams = listOf(
                    TypeParameter("K", upperBounds(
                            TypeSubstitution(
                                    comparable.forceDynamicApply(
                                            Wildcard.Bounded(SelfSubstitution, LOWER)
                                    ).holder()
                            )
                    )),
                    TypeParameter("V", defaultTypeBounds)
            ),
            superTypes = listOf(
                map.forceDynamicApply(
                    ParamSubstitution(0), ParamSubstitution(1)
                )
            )
        )

        val person = createDirect(
            fullName = "Person",
            superTypes = listOf(
                rootType.holder(),
                comparableTo("Person")
            )
        )

        val boss = createDirect("Boss", person)

        val ceo = createDirect("Ceo", boss)

        val function = createTemplate(
                fullName = "Function",
                typeParams = listOf(
                        TypeParameter("I", defaultTypeBounds),
                        TypeParameter("O", defaultTypeBounds)
                ),
                superTypes = listOf(rootType),
                samType = SamType.GenericSam(
                        explicit = true,
                        inputs = listOf(ParamSubstitution(0)),
                        output = ParamSubstitution(1)
                )
        )

        val collector = createTemplate(
            fullName = "Collector",
            typeParams = listOf(
                    TypeParameter("T", defaultTypeBounds),
                    TypeParameter("A", defaultTypeBounds),
                    TypeParameter("R", defaultTypeBounds)
            ),
            superTypes = listOf(rootType)
        )

        val supplier = createTemplate(
            fullName = "Supplier",
            typeParams = listOf(
                    TypeParameter("T", defaultTypeBounds)
            ),
            superTypes = listOf(rootType),
            samType = SamType.GenericSam(
                    explicit = true,
                    inputs = emptyList(),
                    output = ParamSubstitution(0)
            )
        )

    }

}