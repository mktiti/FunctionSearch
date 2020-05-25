@file:Suppress("UNUSED_VARIABLE")

package com.mktiti.fsearch.core.repo // For easier extension

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.SelfSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.BoundedWildcard.LowerBound
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeInfo
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.type.upperBounds
import com.mktiti.fsearch.core.util.forceDynamicApply
import com.mktiti.fsearch.core.util.forceStaticApply
import com.mktiti.fsearch.core.util.listType

fun createTestRepo(): MutableTypeRepo {

    return SetTypeRepo(
            funTypeInfo = TypeInfo("TestFun", emptyList(), ""),
            rootInfo = TypeInfo("TestRoot", emptyList(), "")
    ).apply {

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

        val selfComparable: (DirectType) -> NonGenericType = { self ->
            comparable.forceStaticApply(self)
        }

        val int = createSelfRefDirect(
            fullName = "Int",
            superCreators = listOf(
                { _ -> rootType },
                selfComparable
            )
        )

        val int64 = createDirect("Int64", int)

        val char = createDirect("Char", rootType)

        val charSeq = createDirect("CharSequence", list.forceStaticApply(char))

        val string = createSelfRefDirect(
            fullName = "String",
            superCreators = listOf(
                { _ -> rootType },
                selfComparable,
                { _ -> charSeq }
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
                    DynamicTypeSubstitution(
                        pair.forceDynamicApply(
                            ParamSubstitution(0),
                            ParamSubstitution(1)
                        )
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
                            DynamicTypeSubstitution(
                                    comparable.forceDynamicApply(
                                            LowerBound(SelfSubstitution)
                                    )
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

        val person = createSelfRefDirect(
            fullName = "Person",
            superCreators = listOf(
                { _ -> rootType },
                selfComparable
            )
        )

        val boss = createDirect("Boss", person)

        val ceo = createDirect("Ceo", boss)

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
            superTypes = listOf(rootType)
        )

    }

}