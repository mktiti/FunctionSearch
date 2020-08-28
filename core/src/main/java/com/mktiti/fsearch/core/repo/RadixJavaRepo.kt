package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType
import com.mktiti.fsearch.core.util.forceDynamicApply
import com.mktiti.fsearch.core.util.forceStaticApply
import com.mktiti.fsearch.util.EnumMap
import com.mktiti.fsearch.util.PrefixTree

class RadixJavaRepo(
        artifact: String,
        infoRepo: JavaInfoRepo,
        directs: PrefixTree<String, DirectType>
) : JavaRepo {

    override val objectType = directs[infoRepo.objectType]!!
    override val voidType = DirectType(infoRepo.voidType.full(artifact), emptyList(), samType = null)
    private val arrayTemplate = TypeTemplate(
            info = infoRepo.arrayType.full(artifact),
            superTypes = listOf(SuperType.StaticSuper.EagerStatic(objectType)),
            typeParams = listOf(TypeParameter("X", TypeBounds(setOf(StaticTypeSubstitution(objectType))))),
            samType = null
    )

    private val primitiveMap: EnumMap<PrimitiveType, DirectType> = EnumMap.eager { primitive ->
        DirectType(infoRepo.primitive(primitive).full(artifact), emptyList(), samType = null)
    }

    private val boxedMap: EnumMap<PrimitiveType, DirectType> = EnumMap.eager { primitive ->
        directs[infoRepo.boxed(primitive)]!!
    }

    override fun arrayOf(type: Type.NonGenericType): StaticAppliedType
            = arrayTemplate.forceStaticApply(listOf(type))

    override fun arrayOf(arg: ApplicationParameter): Type.DynamicAppliedType
            = arrayTemplate.forceDynamicApply(listOf(arg))

    override fun primitive(primitive: PrimitiveType) = primitiveMap[primitive]

    override fun boxed(primitive: PrimitiveType) = boxedMap[primitive]

}