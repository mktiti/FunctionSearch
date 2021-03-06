package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType
import com.mktiti.fsearch.core.type.TypeHolder.Static.Direct
import com.mktiti.fsearch.core.util.forceDynamicApply
import com.mktiti.fsearch.core.util.forceStaticApply
import com.mktiti.fsearch.util.EnumMap
import com.mktiti.fsearch.util.PrefixTree

class DefaultJavaRepo(
        infoRepo: JavaInfoRepo,
        directProvider: (MinimalInfo) -> DirectType
) : JavaRepo {

    companion object {
        fun fromNullable(infoRepo: JavaInfoRepo, directProvider: (MinimalInfo) -> DirectType?)
                = DefaultJavaRepo(infoRepo) { directProvider(it) ?: error("Critical type $it missing from JCL") }

        fun fromMap(infoRepo: JavaInfoRepo, directMap: Map<MinimalInfo, DirectType>)
                = fromNullable(infoRepo, directMap::get)

        fun fromRadix(infoRepo: JavaInfoRepo, directTree: PrefixTree<String, DirectType>)
                = fromNullable(infoRepo, directTree::get)
    }

    override val objectType = Direct(directProvider(infoRepo.objectType))

    override val voidType = Direct(DirectType(
            minInfo = infoRepo.voidType,
            superTypes = emptyList(),
            samType = null,
            virtual = false
    ))

    private val arrayTemplate = TypeTemplate(
            info = infoRepo.arrayType,
            superTypes = TypeHolder.anyIndirects(objectType.info),
            typeParams = listOf(TypeParameter("X", TypeBounds(setOf(TypeSubstitution(objectType))))),
            samType = null
    )

    private val primitiveMap: EnumMap<PrimitiveType, Direct> = EnumMap.eager { primitive ->
        Direct(
                type = DirectType(
                    minInfo = infoRepo.primitive(primitive),
                    superTypes = emptyList(),
                    samType = null,
                    virtual = false
            )
        )
    }

    private val boxedMap: EnumMap<PrimitiveType, Direct> = EnumMap.eager { primitive ->
        Direct(directProvider(infoRepo.boxed(primitive)))
    }

    override fun arrayOf(type: TypeHolder.Static): StaticAppliedType
            = arrayTemplate.forceStaticApply(listOf(type))

    override fun arrayOf(arg: ApplicationParameter): Type.DynamicAppliedType
            = arrayTemplate.forceDynamicApply(listOf(arg))

    override fun primitive(primitive: PrimitiveType) = primitiveMap[primitive]

    override fun boxed(primitive: PrimitiveType) = boxedMap[primitive]

}