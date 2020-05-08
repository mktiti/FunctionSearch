package repo

import ApplicationParameter
import EnumMap
import PrefixTree
import PrimitiveType
import Type.NonGenericType.*
import TypeBounds
import TypeParameter
import TypeTemplate
import forceDynamicApply
import forceStaticApply
import type.get
import ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution

class RadixJavaRepo(
        artifact: String,
        infoRepo: JavaInfoRepo,
        directs: PrefixTree<String, DirectType>,
        templates: PrefixTree<String, TypeTemplate>
) : JavaRepo {

    override val objectType = directs[infoRepo.objectType]!!
    override val voidType = directs[infoRepo.voidType]!!
    private val arrayTemplate = TypeTemplate(
            info = infoRepo.arrayType.full(artifact),
            superTypes = listOf(SuperType.StaticSuper.EagerStatic(objectType)),
            typeParams = listOf(TypeParameter("X", TypeBounds(setOf(StaticTypeSubstitution(objectType)))))
    )

    private val primitiveMap: EnumMap<PrimitiveType, DirectType> = EnumMap.eager { primitive ->
        DirectType(infoRepo.primitive(primitive).full(artifact), emptyList())
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